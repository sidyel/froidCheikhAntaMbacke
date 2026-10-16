package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.DocumentFacturationDTO;
import com.froidcheikh.ecommerce.entity.*;
import com.froidcheikh.ecommerce.entity.Commande.StatutPaiement;
import com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument;
import com.froidcheikh.ecommerce.entity.DocumentFacturation.StatutDocument;
import com.froidcheikh.ecommerce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentFacturationServiceTest {

    @Mock DocumentFacturationRepository documentRepo;
    @Mock ClientFacturationRepository clientRepo;
    @Mock ClientRepository clientSiteRepo;
    @Mock CommandeRepository commandeRepo;
    @Mock ProduitRepository produitRepo;
    @Mock ParametresSiteRepository parametresRepo;
    @Mock NumeroDocumentService numeroService;
    @Mock CalculDocumentService calculService;
    @Mock MontantEnLettresService lettresService;
    @Mock PdfFacturationService pdfService;

    @InjectMocks
    DocumentFacturationService service;

    @Captor ArgumentCaptor<DocumentFacturation> docCaptor;

    private ParametresSite params;

    @BeforeEach
    void setUp() {
        params = new ParametresSite();
        params.setTauxTvaDefaut(BigDecimal.ZERO);
        params.setPrixAfficheTTC(false);
        params.setPrefixeFacture("FAC");
        params.setPrefixeAvoir("AVO");
        params.setPrefixeDevis("DEV");
        params.setPrefixeBonLivraison("BL");
        params.setDureeValiditeJetonHeures(72);
        params.setMentionExonerationTva("TVA non applicable");
        lenient().when(parametresRepo.findById(1L)).thenReturn(Optional.of(params));
        lenient().when(documentRepo.save(any())).thenAnswer(inv -> {
            DocumentFacturation d = inv.getArgument(0);
            if (d.getId() == null) {
                try {
                    java.lang.reflect.Field f = DocumentFacturation.class.getDeclaredField("id");
                    f.setAccessible(true);
                    f.set(d, 1L);
                } catch (Exception ignored) {}
            }
            return d;
        });
        lenient().when(calculService.calculerTotaux(anyList(), any(), anyBoolean()))
                .thenReturn(new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
    }

    // ---- brouillon modifiable ----

    @Test
    void brouillonEstModifiable() {
        DocumentFacturation doc = brouillonDoc();
        when(documentRepo.findById(1L)).thenReturn(Optional.of(doc));

        DocumentFacturationDTO.CreateRequest req = new DocumentFacturationDTO.CreateRequest();
        req.setTypeDocument(TypeDocument.FACTURE);
        req.setRemiseTotalePourcent(BigDecimal.ZERO);

        service.modifierBrouillon(1L, req, "admin");
        verify(documentRepo, atLeastOnce()).save(any());
    }

    @Test
    void documentEmisNonModifiable() {
        DocumentFacturation doc = brouillonDoc();
        doc.setStatut(StatutDocument.EMIS);
        when(documentRepo.findById(1L)).thenReturn(Optional.of(doc));

        DocumentFacturationDTO.CreateRequest req = new DocumentFacturationDTO.CreateRequest();
        req.setTypeDocument(TypeDocument.FACTURE);

        assertThatThrownBy(() -> service.modifierBrouillon(1L, req, "admin"))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---- émission → figement ----

    @Test
    void emissionFigeNumeroEtPdf() {
        DocumentFacturation doc = brouillonDoc();
        when(documentRepo.findById(1L)).thenReturn(Optional.of(doc));
        when(numeroService.attribuerNumero(TypeDocument.FACTURE, "FAC")).thenReturn("FAC-2026-0001");
        when(pdfService.generer(any(), any(), eq(false))).thenReturn(new byte[]{1, 2, 3});
        when(lettresService.convertir(any())).thenReturn("Arrêtée ...");

        service.emettre(1L, "admin", false);

        verify(documentRepo).save(docCaptor.capture());
        DocumentFacturation saved = docCaptor.getValue();
        assertThat(saved.getNumero()).isEqualTo("FAC-2026-0001");
        assertThat(saved.getStatut()).isEqualTo(StatutDocument.EMIS);
        assertThat(saved.getPdfBytes()).isNotNull();
        assertThat(saved.getPdfSha256()).isNotNull();
    }

    @Test
    void deuxiemeFactureCommandeInterdit() {
        DocumentFacturation doc = brouillonDoc();
        Commande cmd = commande(250_000);
        doc.setCommande(cmd);
        when(documentRepo.findById(1L)).thenReturn(Optional.of(doc));
        when(documentRepo.countFacturesEmisesParCommande(cmd.getIdCommande())).thenReturn(1L);

        assertThatThrownBy(() -> service.emettre(1L, "admin", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("facture émise existe déjà");
    }

    // ---- devis → facture ----

    @Test
    void conversionDevisEnFacture() {
        // Devis id=2L, nouvelle facture id=1L (attribué par le mock save)
        DocumentFacturation devis = brouillonDoc();
        setId(devis, 2L);
        devis.setTypeDocument(TypeDocument.DEVIS);
        devis.setStatut(StatutDocument.EMIS);
        devis.setNumero("DEV-2026-0001");

        DocumentFacturation nouvelleFacture = brouillonDoc(); // id=1L par défaut

        // findById(2L) → devis, findById(1L) → facture brouillon (pour emettre)
        when(documentRepo.findById(2L)).thenReturn(Optional.of(devis));
        when(documentRepo.findById(1L)).thenReturn(Optional.of(nouvelleFacture));
        when(numeroService.attribuerNumero(TypeDocument.FACTURE, "FAC")).thenReturn("FAC-2026-0001");
        when(pdfService.generer(any(), any(), anyBoolean())).thenReturn(new byte[]{1});
        when(lettresService.convertir(any())).thenReturn("Arrêtée ...");

        service.convertirDevisEnFacture(2L, "admin");

        // Le devis doit être marqué CONVERTI
        verify(documentRepo, atLeast(2)).save(docCaptor.capture());
        boolean devisConverti = docCaptor.getAllValues().stream()
                .anyMatch(d -> d.getStatut() == StatutDocument.CONVERTI);
        assertThat(devisConverti).isTrue();
    }

    private void setId(DocumentFacturation d, long id) {
        try {
            java.lang.reflect.Field f = DocumentFacturation.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(d, id);
        } catch (Exception ignored) {}
    }

    // ---- statut paiement commande ----

    @Test
    void reglementTotalMarcheCommandePayee() {
        Commande cmd = commande(250_000);
        DocumentFacturation doc = emis(250_000, cmd);
        when(documentRepo.findById(1L)).thenReturn(Optional.of(doc));
        when(commandeRepo.save(any())).thenReturn(cmd);

        DocumentFacturationDTO.ReglementRequest req = new DocumentFacturationDTO.ReglementRequest();
        req.setDateReglement(LocalDate.now());
        req.setMontant(bd("250000"));
        req.setMode(com.froidcheikh.ecommerce.entity.ReglementDocument.ModeReglement.WAVE);

        service.ajouterReglement(1L, req, "admin");

        assertThat(cmd.getStatutPaiement()).isEqualTo(StatutPaiement.PAYEE);
    }

    @Test
    void reglementPartielCommandeNonPayee() {
        Commande cmd = commande(250_000);
        DocumentFacturation doc = emis(250_000, cmd);
        when(documentRepo.findById(1L)).thenReturn(Optional.of(doc));
        when(commandeRepo.save(any())).thenReturn(cmd);

        DocumentFacturationDTO.ReglementRequest req = new DocumentFacturationDTO.ReglementRequest();
        req.setDateReglement(LocalDate.now());
        req.setMontant(bd("100000")); // partiel
        req.setMode(com.froidcheikh.ecommerce.entity.ReglementDocument.ModeReglement.ESPECES);

        service.ajouterReglement(1L, req, "admin");

        // 100 000 < 250 000 → toujours NON_PAYEE (pas de PARTIELLEMENT_PAYEE sur Commande)
        assertThat(cmd.getStatutPaiement()).isEqualTo(StatutPaiement.NON_PAYEE);
    }

    // ---- facture commande = montant exact ----

    @Test
    void factureCommandeMontantExact() {
        // Scénario : commande 250 000 FCFA, facture générée avec la même somme
        Commande cmd = commande(250_000);
        DocumentFacturation doc = brouillonDoc();
        doc.setCommande(cmd);

        LigneDocument l = new LigneDocument();
        l.setDesignation("Climatiseur");
        l.setQuantite(bd("1"));
        l.setPrixUnitaireHT(bd("250000"));
        l.setTauxTva(BigDecimal.ZERO);
        l.setRemisePourcent(BigDecimal.ZERO);
        l.setMontantHT(bd("250000"));
        l.setMontantTva(BigDecimal.ZERO);
        l.setMontantTTC(bd("250000"));
        doc.getLignes().add(l);
        doc.setMontantHT(bd("250000"));
        doc.setMontantTTC(bd("250000"));
        doc.setMontantTva(BigDecimal.ZERO);

        BigDecimal montant = bd("250000");
        when(calculService.calculerTotaux(anyList(), any(), anyBoolean()))
                .thenReturn(new BigDecimal[]{montant, BigDecimal.ZERO, montant});
        when(documentRepo.findById(1L)).thenReturn(Optional.of(doc));
        when(documentRepo.countFacturesEmisesParCommande(cmd.getIdCommande())).thenReturn(0L);
        when(numeroService.attribuerNumero(any(), any())).thenReturn("FAC-2026-0001");
        when(pdfService.generer(any(), any(), eq(false))).thenReturn(new byte[]{1});
        when(lettresService.convertir(any())).thenReturn("Arrêtée ...");

        service.emettre(1L, "admin", false);

        verify(documentRepo).save(docCaptor.capture());
        DocumentFacturation saved = docCaptor.getValue();
        assertThat(saved.getMontantTTC()).isEqualByComparingTo(
                BigDecimal.valueOf(cmd.getMontantTotal().longValue()));
    }

    // ---- GESTIONNAIRE + BL ----

    @Test
    void gestionnaireEmetBonDeLivraison() {
        DocumentFacturation bl = brouillonDoc();
        bl.setTypeDocument(TypeDocument.BON_LIVRAISON);
        when(documentRepo.findById(1L)).thenReturn(Optional.of(bl));
        when(numeroService.attribuerNumero(TypeDocument.BON_LIVRAISON, "BL")).thenReturn("BL-2026-0001");
        when(pdfService.generer(any(), any(), eq(false))).thenReturn(new byte[]{1});
        when(lettresService.convertir(any())).thenReturn("zéro franc CFA");

        service.emettre(1L, "gestionnaire", true);

        verify(documentRepo).save(docCaptor.capture());
        assertThat(docCaptor.getValue().getStatut()).isEqualTo(StatutDocument.EMIS);
    }

    @Test
    void gestionnaireRefuseEmettreFacture() {
        DocumentFacturation facture = brouillonDoc(); // type = FACTURE
        when(documentRepo.findById(1L)).thenReturn(Optional.of(facture));

        assertThatThrownBy(() -> service.emettre(1L, "gestionnaire", true))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("bons de livraison");
    }

    @Test
    void gestionnaireRefuseEmettreDevis() {
        DocumentFacturation devis = brouillonDoc();
        devis.setTypeDocument(TypeDocument.DEVIS);
        when(documentRepo.findById(1L)).thenReturn(Optional.of(devis));

        assertThatThrownBy(() -> service.emettre(1L, "gestionnaire", true))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---- helpers ----

    private DocumentFacturation brouillonDoc() {
        DocumentFacturation d = new DocumentFacturation();
        d.setTypeDocument(TypeDocument.FACTURE);
        d.setStatut(StatutDocument.BROUILLON);
        d.setTauxTvaDefaut(BigDecimal.ZERO);
        d.setPrixAfficheTTC(false);
        d.setRemiseTotalePourcent(BigDecimal.ZERO);
        d.setMontantHT(BigDecimal.ZERO);
        d.setMontantTva(BigDecimal.ZERO);
        d.setMontantTTC(BigDecimal.ZERO);
        d.setDateCreation(LocalDateTime.now());
        try {
            java.lang.reflect.Field f = DocumentFacturation.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(d, 1L);
        } catch (Exception ignored) {}
        return d;
    }

    private DocumentFacturation emis(long montant, Commande cmd) {
        DocumentFacturation d = brouillonDoc();
        d.setStatut(StatutDocument.EMIS);
        d.setNumero("FAC-2026-0001");
        d.setMontantTTC(bd(String.valueOf(montant)));
        d.setCommande(cmd);
        d.setPdfBytes(new byte[]{1});
        return d;
    }

    private Commande commande(long montant) {
        Commande c = new Commande();
        try {
            java.lang.reflect.Field f = Commande.class.getDeclaredField("idCommande");
            f.setAccessible(true);
            f.set(c, 42L);
        } catch (Exception ignored) {}
        c.setMontantTotal(bd(String.valueOf(montant)));
        c.setStatutPaiement(StatutPaiement.NON_PAYEE);
        c.setNumeroCommande("CMD-2026-001");
        return c;
    }

    private BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
