package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.CommandeDTO;
import com.froidcheikh.ecommerce.entity.*;
import com.froidcheikh.ecommerce.exception.ResourceNotFoundException;
import com.froidcheikh.ecommerce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommandeServiceTest {

    @Mock CommandeRepository commandeRepository;
    @Mock ClientRepository clientRepository;
    @Mock ProduitRepository produitRepository; // requis par @InjectMocks
    @Mock PaiementRepository paiementRepository;
    @Mock HistoriqueStatutCommandeRepository historiqueRepository;
    @Mock ProduitService produitService;
    @Mock CalculCommandeService calculCommandeService;
    @Mock PrixService prixService;
    @Mock ModelMapper modelMapper;

    @InjectMocks CommandeService commandeService;

    private Commande commandeEnAttente;
    private Commande commandeConfirmee;
    private Produit produit;

    @BeforeEach
    void setUp() {
        produit = new Produit();
        produit.setIdProduit(1L);
        produit.setStockDisponible(5);
        produit.setPrix(BigDecimal.valueOf(100));

        LigneCommande ligne = new LigneCommande();
        ligne.setProduit(produit);
        ligne.setQuantite(2);
        ligne.setPrixUnitaire(BigDecimal.valueOf(100));
        ligne.setSousTotal(BigDecimal.valueOf(200));
        ligne.setNomProduitCommande("Produit Test");

        commandeEnAttente = new Commande();
        commandeEnAttente.setIdCommande(1L);
        commandeEnAttente.setStatutCommande(Commande.StatutCommande.EN_ATTENTE);
        commandeEnAttente.setStatutPaiement(Commande.StatutPaiement.NON_PAYEE);
        commandeEnAttente.setLignesCommande(List.of(ligne));
        commandeEnAttente.setMontantTotal(BigDecimal.valueOf(200));

        commandeConfirmee = new Commande();
        commandeConfirmee.setIdCommande(2L);
        commandeConfirmee.setStatutCommande(Commande.StatutCommande.CONFIRMEE);
        commandeConfirmee.setStatutPaiement(Commande.StatutPaiement.NON_PAYEE);
        commandeConfirmee.setLignesCommande(List.of(ligne));
        commandeConfirmee.setMontantTotal(BigDecimal.valueOf(200));

        // ModelMapper renvoie un DTO vide par défaut (on vérifie le comportement, pas la sérialisation)
        when(modelMapper.map(any(Commande.class), eq(CommandeDTO.class)))
                .thenAnswer(inv -> {
                    CommandeDTO dto = new CommandeDTO();
                    Commande c = inv.getArgument(0);
                    dto.setStatutCommande(c.getStatutCommande());
                    dto.setStatutPaiement(c.getStatutPaiement());
                    dto.setIdCommande(c.getIdCommande());
                    return dto;
                });
        when(historiqueRepository.findByCommandeIdCommandeOrderByDateChangementAsc(any()))
                .thenReturn(new ArrayList<>());
    }

    // --- CAS 1 : EN_ATTENTE → CONFIRMEE : stock inchangé (déjà décrémenté à la création) ---
    @Test
    void enAttente_vers_confirmee_ne_modifie_pas_le_stock() {
        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commandeEnAttente));
        when(commandeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CommandeDTO result = commandeService.updateStatutCommande(1L, Commande.StatutCommande.CONFIRMEE);

        assertThat(result.getStatutCommande()).isEqualTo(Commande.StatutCommande.CONFIRMEE);
        // le stock du produit n'est pas touché
        verify(produitRepository, never()).save(any());
    }

    // --- CAS 2 : CONFIRMEE → LIVREE : OK ---
    @Test
    void confirmee_vers_livree_ok() {
        when(commandeRepository.findById(2L)).thenReturn(Optional.of(commandeConfirmee));
        when(commandeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CommandeDTO result = commandeService.updateStatutCommande(2L, Commande.StatutCommande.LIVREE);

        assertThat(result.getStatutCommande()).isEqualTo(Commande.StatutCommande.LIVREE);
        verify(historiqueRepository, times(1)).save(any(HistoriqueStatutCommande.class));
    }

    // --- CAS 3 : CONFIRMEE → ANNULEE (admin) : produitService.updateStock appelé ---
    @Test
    void confirmee_vers_annulee_admin_restaure_stock() {
        when(commandeRepository.findById(2L)).thenReturn(Optional.of(commandeConfirmee));
        when(commandeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        commandeService.updateStatutCommande(2L, Commande.StatutCommande.ANNULEE, "Test admin", "admin@test.com");

        // restaurerStock délègue à produitService.updateStock(id, stockRestauré)
        verify(produitService, times(1)).updateStock(eq(1L),
                eq(produit.getStockDisponible() + 2));
        verify(historiqueRepository, times(1)).save(argThat(h ->
                h.getNouveauStatut() == Commande.StatutCommande.ANNULEE
                        && "admin@test.com".equals(h.getAuteur())));
    }

    // --- CAS 4 : EN_ATTENTE → ANNULEE (admin) : produitService.updateStock appelé ---
    @Test
    void enAttente_vers_annulee_admin_restaure_stock() {
        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commandeEnAttente));
        when(commandeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        commandeService.updateStatutCommande(1L, Commande.StatutCommande.ANNULEE);

        verify(produitService, times(1)).updateStock(eq(1L),
                eq(produit.getStockDisponible() + 2));
    }

    // --- CAS 5 : EN_ATTENTE → ANNULEE (client) : autorisé, stock restauré ---
    @Test
    void client_peut_annuler_sa_commande_en_attente() {
        Client client = new Client();
        client.setIdClient(42L);
        commandeEnAttente.setClient(client);

        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commandeEnAttente));
        when(commandeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CommandeDTO result = commandeService.annulerCommandeClient(1L, 42L, "Je n'en veux plus");

        assertThat(result.getStatutCommande()).isEqualTo(Commande.StatutCommande.ANNULEE);
        verify(produitService, times(1)).updateStock(eq(1L),
                eq(produit.getStockDisponible() + 2));
        verify(historiqueRepository, times(1)).save(argThat(h ->
                "CLIENT".equals(h.getAuteur()) && h.getNouveauStatut() == Commande.StatutCommande.ANNULEE));
    }

    // --- CAS 6 : CONFIRMEE → ANNULEE client : refusé ---
    @Test
    void client_ne_peut_pas_annuler_commande_confirmee() {
        Client client = new Client();
        client.setIdClient(42L);
        commandeConfirmee.setClient(client);

        when(commandeRepository.findById(2L)).thenReturn(Optional.of(commandeConfirmee));

        assertThatThrownBy(() -> commandeService.annulerCommandeClient(2L, 42L, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("en attente");
    }

    // --- CAS 7 : LIVREE → ANNULEE : refusé ---
    @Test
    void livree_vers_annulee_refuse() {
        Commande commandeLivree = new Commande();
        commandeLivree.setIdCommande(3L);
        commandeLivree.setStatutCommande(Commande.StatutCommande.LIVREE);
        commandeLivree.setLignesCommande(new ArrayList<>());
        when(commandeRepository.findById(3L)).thenReturn(Optional.of(commandeLivree));

        assertThatThrownBy(() -> commandeService.updateStatutCommande(3L, Commande.StatutCommande.ANNULEE))
                .isInstanceOf(IllegalStateException.class);
    }

    // --- CAS 8 : ANNULEE → CONFIRMEE : refusé ---
    @Test
    void annulee_vers_confirmee_refuse() {
        Commande commandeAnnulee = new Commande();
        commandeAnnulee.setIdCommande(4L);
        commandeAnnulee.setStatutCommande(Commande.StatutCommande.ANNULEE);
        commandeAnnulee.setLignesCommande(new ArrayList<>());
        when(commandeRepository.findById(4L)).thenReturn(Optional.of(commandeAnnulee));

        assertThatThrownBy(() -> commandeService.updateStatutCommande(4L, Commande.StatutCommande.CONFIRMEE))
                .isInstanceOf(IllegalStateException.class);
    }

    // --- CAS 9 : annulerCommandeClient — commande d'un autre client : refusé ---
    @Test
    void client_ne_peut_pas_annuler_commande_dun_autre() {
        Client client = new Client();
        client.setIdClient(99L);
        commandeEnAttente.setClient(client);
        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commandeEnAttente));

        assertThatThrownBy(() -> commandeService.annulerCommandeClient(1L, 42L, ""))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    // --- CAS 10 : Suivi invité — bon téléphone → DTO limité ---
    @Test
    void suivi_invite_bon_telephone_retourne_dto_limite() {
        // Commande invitée : client == null, téléphone dans telephoneInvite
        commandeEnAttente.setTelephoneInvite("77 123 45 67");
        commandeEnAttente.setNumeroCommande("CMD-001");

        when(commandeRepository.findByNumeroCommande("CMD-001"))
                .thenReturn(Optional.of(commandeEnAttente));

        CommandeDTO result = commandeService.getSuiviCommande("CMD-001", "77 123 45 67");
        assertThat(result.getStatutCommande()).isEqualTo(Commande.StatutCommande.EN_ATTENTE);
    }

    // --- CAS 11 : Suivi invité — mauvais téléphone → 404 ---
    @Test
    void suivi_invite_mauvais_telephone_leve_not_found() {
        commandeEnAttente.setTelephoneInvite("77 123 45 67");
        commandeEnAttente.setNumeroCommande("CMD-001");

        when(commandeRepository.findByNumeroCommande("CMD-001"))
                .thenReturn(Optional.of(commandeEnAttente));

        assertThatThrownBy(() -> commandeService.getSuiviCommande("CMD-001", "77 999 99 99"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
