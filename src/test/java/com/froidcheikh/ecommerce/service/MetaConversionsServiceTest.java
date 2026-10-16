package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.CommandeDTO;
import com.froidcheikh.ecommerce.entity.Commande;
import com.froidcheikh.ecommerce.entity.ParametresSite;
import com.froidcheikh.ecommerce.repository.ParametresSiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetaConversionsServiceTest {

    @Mock ParametresSiteRepository parametresSiteRepository;

    MetaConversionsService service;

    @BeforeEach
    void setUp() {
        service = new MetaConversionsService(parametresSiteRepository);
    }

    // ── Hachage SHA-256 ────────────────────────────────────────────────────────

    @Test
    void hashSha256_email_connu() {
        // Vecteur de référence Meta
        assertThat(MetaConversionsService.hashSha256("test@example.com"))
                .isEqualTo("973dfe463ec85785f5f95af5ba3906eedb2d931c24e69824a89ea65dba4e813b");
    }

    @Test
    void hashSha256_vide_retourne_vide() {
        assertThat(MetaConversionsService.hashSha256("")).isEmpty();
        assertThat(MetaConversionsService.hashSha256(null)).isEmpty();
    }

    @Test
    void hashSha256_produit_64_chars_hex() {
        String h = MetaConversionsService.hashSha256("hello");
        assertThat(h).hasSize(64).matches("[0-9a-f]+");
    }

    // ── Normalisation email ────────────────────────────────────────────────────

    @Test
    void normalizeEmail_met_en_minuscules_et_trim() {
        assertThat(MetaConversionsService.normalizeEmail("  Test@Example.COM  ")).isEqualTo("test@example.com");
    }

    // ── Normalisation téléphone ────────────────────────────────────────────────

    @Test
    void normalizePhone_ajoute_indicatif_senegal_pour_numero_local_7() {
        assertThat(MetaConversionsService.normalizePhone("77 123 45 67")).isEqualTo("221771234567");
    }

    @Test
    void normalizePhone_ajoute_indicatif_senegal_pour_numero_local_3() {
        assertThat(MetaConversionsService.normalizePhone("33 123 45 67")).isEqualTo("221331234567");
    }

    @Test
    void normalizePhone_ne_double_pas_indicatif_deja_present() {
        assertThat(MetaConversionsService.normalizePhone("+221 77 123 45 67")).isEqualTo("221771234567");
    }

    @Test
    void normalizePhone_vide_retourne_vide() {
        assertThat(MetaConversionsService.normalizePhone(null)).isEmpty();
        assertThat(MetaConversionsService.normalizePhone("")).isEmpty();
    }

    // ── Aucun envoi sans consentement ─────────────────────────────────────────

    @Test
    void sendPurchaseEvent_sans_consentement_ne_contacte_pas_api() {
        ReflectionTestUtils.setField(service, "accessToken", "fake-token");

        Commande commande = new Commande();
        commande.setIdCommande(1L);
        commande.setMontantTotal(new BigDecimal("150000"));

        CommandeDTO dto = new CommandeDTO();
        dto.setConsentementCookies(null);

        service.sendPurchaseEvent(commande, dto, "127.0.0.1", "Mozilla/5.0");

        verifyNoInteractions(parametresSiteRepository);
    }

    @Test
    void sendPurchaseEvent_consentement_refuse_ne_contacte_pas_api() {
        ReflectionTestUtils.setField(service, "accessToken", "fake-token");

        Commande commande = new Commande();
        commande.setIdCommande(2L);
        commande.setMontantTotal(new BigDecimal("100000"));

        CommandeDTO dto = new CommandeDTO();
        dto.setConsentementCookies(false);

        service.sendPurchaseEvent(commande, dto, "127.0.0.1", "Mozilla/5.0");

        verifyNoInteractions(parametresSiteRepository);
    }

    @Test
    void sendPurchaseEvent_sans_token_ne_contacte_pas_api() {
        ReflectionTestUtils.setField(service, "accessToken", "");

        Commande commande = new Commande();
        commande.setIdCommande(3L);

        CommandeDTO dto = new CommandeDTO();
        dto.setConsentementCookies(true);

        service.sendPurchaseEvent(commande, dto, "127.0.0.1", "Mozilla/5.0");

        verifyNoInteractions(parametresSiteRepository);
    }

    @Test
    void sendPurchaseEvent_sans_pixel_id_ne_contacte_pas_api() {
        ReflectionTestUtils.setField(service, "accessToken", "fake-token");

        ParametresSite ps = new ParametresSite();
        ps.setMetaPixelId("");
        when(parametresSiteRepository.findById(1L)).thenReturn(Optional.of(ps));

        Commande commande = new Commande();
        commande.setIdCommande(4L);
        commande.setMontantTotal(new BigDecimal("80000"));

        CommandeDTO dto = new CommandeDTO();
        dto.setConsentementCookies(true);

        service.sendPurchaseEvent(commande, dto, "127.0.0.1", "Mozilla/5.0");
        // Aucune exception, aucun appel HTTP — si pixelId vide, retour immédiat
    }
}
