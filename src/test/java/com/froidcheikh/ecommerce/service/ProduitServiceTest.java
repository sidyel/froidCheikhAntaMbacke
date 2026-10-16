package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.ProduitDTO;
import com.froidcheikh.ecommerce.entity.AttributProduit;
import com.froidcheikh.ecommerce.entity.Produit;
import com.froidcheikh.ecommerce.repository.CategorieRepository;
import com.froidcheikh.ecommerce.repository.MarqueRepository;
import com.froidcheikh.ecommerce.repository.ProduitRepository;
import com.froidcheikh.ecommerce.repository.ImageProduitRepository;
import com.froidcheikh.ecommerce.service.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProduitServiceTest {

    @Mock private ProduitRepository produitRepository;
    @Mock private CategorieRepository categorieRepository;
    @Mock private MarqueRepository marqueRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private PrixService prixService;
    @Mock private ImageProduitRepository imageProduitRepository;
    @Mock private FileService fileService;
    @Mock private CatalogueFeedService catalogueFeedService;

    @InjectMocks private ProduitService service;

    // ── helpers ──────────────────────────────────────────────────────────────

    private Produit produitAvecAttributs(Long id, String... pairs) {
        Produit p = new Produit();
        p.setIdProduit(id);
        p.setNomProduit("Clim Test");
        p.setPrix(new BigDecimal("150000"));
        p.setAttributs(new ArrayList<>());
        for (int i = 0; i < pairs.length - 1; i += 2) {
            AttributProduit a = new AttributProduit();
            a.setIdAttribut((long) (i / 2 + 1));
            a.setNomAttribut(pairs[i]);
            a.setValeurAttribut(pairs[i + 1]);
            a.setProduit(p);
            p.getAttributs().add(a);
        }
        return p;
    }

    private ProduitDTO dtoBasique(String nom) {
        ProduitDTO dto = new ProduitDTO();
        dto.setNomProduit(nom);
        dto.setPrix(new BigDecimal("150000"));
        return dto;
    }

    private ProduitDTO.AttributProduitDTO attrDTO(String nom, String valeur) {
        return new ProduitDTO.AttributProduitDTO(null, nom, valeur, null, null);
    }

    private void mockSave(Produit produit) {
        when(produitRepository.findById(produit.getIdProduit())).thenReturn(Optional.of(produit));
        when(produitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(prixService.calculerPrix(any())).thenReturn(
                new PrixService.PrixResultat(produit.getPrix(), null, null, null, null, null));
        when(modelMapper.map(any(Produit.class), any())).thenAnswer(inv -> new ProduitDTO());
    }

    // ── tests : attributs ────────────────────────────────────────────────────

    @Test
    void updateProduit_preserveAttributsQuandDtoAttributsEstNull() {
        Produit produit = produitAvecAttributs(1L, "Gaz", "R32", "Inverter", "Oui");
        ProduitDTO dto = dtoBasique("Clim Samsung v2");
        dto.setAttributs(null); // null = ne pas toucher
        mockSave(produit);

        service.updateProduit(1L, dto);

        assertThat(produit.getAttributs()).hasSize(2);
        assertThat(produit.getAttributs().get(0).getNomAttribut()).isEqualTo("Gaz");
    }

    @Test
    void updateProduit_remplaceLesAttributsQuandDtoAttributsNonNull() {
        Produit produit = produitAvecAttributs(1L, "AncienAttr", "AncienneValeur");
        ProduitDTO dto = dtoBasique("Clim Samsung v2");
        dto.setAttributs(List.of(
                attrDTO("Gaz réfrigérant", "R32"),
                attrDTO("Inverter", "Oui"),
                attrDTO("Type", "split")
        ));
        mockSave(produit);

        service.updateProduit(1L, dto);

        assertThat(produit.getAttributs()).hasSize(3);
        assertThat(produit.getAttributs()).extracting(AttributProduit::getNomAttribut)
                .containsExactly("Gaz réfrigérant", "Inverter", "Type");
    }

    @Test
    void updateProduit_supprimeTousAttributsQuandDtoListeVide() {
        Produit produit = produitAvecAttributs(1L, "Gaz", "R32");
        ProduitDTO dto = dtoBasique("Clim Samsung v2");
        dto.setAttributs(List.of()); // liste vide = tout supprimer
        mockSave(produit);

        service.updateProduit(1L, dto);

        assertThat(produit.getAttributs()).isEmpty();
    }

    @Test
    void updateProduit_chaqueProduitAjouteRefereLeProduItParent() {
        Produit produit = produitAvecAttributs(1L);
        ProduitDTO dto = dtoBasique("Test");
        dto.setAttributs(List.of(attrDTO("Puissance", "12000 BTU")));
        mockSave(produit);

        service.updateProduit(1L, dto);

        assertThat(produit.getAttributs()).hasSize(1);
        assertThat(produit.getAttributs().get(0).getProduit()).isSameAs(produit);
    }
}
