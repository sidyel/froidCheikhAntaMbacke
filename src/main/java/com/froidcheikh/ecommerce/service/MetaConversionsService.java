package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.CommandeDTO;
import com.froidcheikh.ecommerce.entity.Commande;
import com.froidcheikh.ecommerce.entity.LigneCommande;
import com.froidcheikh.ecommerce.entity.ParametresSite;
import com.froidcheikh.ecommerce.repository.ParametresSiteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Envoie l'événement Purchase à l'API Conversions Meta côté serveur.
 *
 * Désactivé automatiquement si META_CONVERSIONS_TOKEN est vide ou si
 * metaPixelId n'est pas configuré dans ParametresSite.
 * N'envoie rien si le client n'a pas accepté les cookies (consentementCookies).
 */
@Slf4j
@Service
public class MetaConversionsService {

    private static final String API_URL = "https://graph.facebook.com/v19.0/%s/events";

    @Value("${meta.conversions.token:}")
    private String accessToken;

    private final ParametresSiteRepository parametresSiteRepository;

    public MetaConversionsService(ParametresSiteRepository parametresSiteRepository) {
        this.parametresSiteRepository = parametresSiteRepository;
    }

    /**
     * Envoie l'événement Purchase de manière asynchrone.
     *
     * @param commande      l'entité commande sauvegardée
     * @param commandeDTO   DTO d'origine (contient consentement, fbp, fbc, ip, ua)
     * @param ipClient      adresse IP extraite de la requête (X-Forwarded-For pris en compte)
     * @param userAgent     User-Agent du navigateur du client
     */
    @Async
    public void sendPurchaseEvent(Commande commande, CommandeDTO commandeDTO,
                                  String ipClient, String userAgent) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        if (!Boolean.TRUE.equals(commandeDTO.getConsentementCookies())) {
            log.debug("API Conversions : consentement absent — événement non envoyé (commande {})",
                    commande.getIdCommande());
            return;
        }

        String pixelId = parametresSiteRepository.findById(1L)
                .map(ParametresSite::getMetaPixelId)
                .filter(id -> id != null && !id.isBlank())
                .orElse(null);
        if (pixelId == null) {
            return;
        }

        String testCode = parametresSiteRepository.findById(1L)
                .map(ParametresSite::getMetaConversionsTestCode)
                .filter(c -> c != null && !c.isBlank())
                .orElse(null);

        try {
            String payload = buildPayload(commande, commandeDTO, ipClient, userAgent, testCode);
            String url = String.format(API_URL, pixelId) + "?access_token=" + accessToken;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("API Conversions Meta : Purchase envoyé (commande {}, pixel {})",
                        commande.getIdCommande(), pixelId);
            } else {
                log.warn("API Conversions Meta : réponse {} pour la commande {} — {}",
                        response.statusCode(), commande.getIdCommande(), response.body());
            }
        } catch (Exception e) {
            log.error("API Conversions Meta : erreur envoi commande {} — {}",
                    commande.getIdCommande(), e.getMessage());
        }
    }

    // ── Construction du payload JSON ──────────────────────────────────────────

    private String buildPayload(Commande commande, CommandeDTO dto,
                                String ipClient, String userAgent, String testCode) {
        long eventTime = Instant.now().getEpochSecond();
        String eventId = "purchase-" + commande.getIdCommande();

        // Données client hachées
        String emailHash = hashSha256(normalizeEmail(resolveEmail(commande, dto)));
        String phoneHash  = hashSha256(normalizePhone(resolvePhone(commande, dto)));
        String prenomHash = hashSha256(normalizeText(resolvePrenom(commande, dto)));
        String nomHash    = hashSha256(normalizeText(resolveNom(commande, dto)));

        // content_ids (IDs produits achetés)
        List<String> contentIds = commande.getLignesCommande().stream()
                .filter(l -> l.getProduit() != null)
                .map(l -> String.valueOf(l.getProduit().getIdProduit()))
                .collect(Collectors.toList());
        int numItems = commande.getLignesCommande() != null ? commande.getLignesCommande().size() : 0;

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"data\":[{");
        sb.append("\"event_name\":\"Purchase\",");
        sb.append("\"event_time\":").append(eventTime).append(",");
        sb.append("\"event_id\":\"").append(jsonEscape(eventId)).append("\",");
        sb.append("\"action_source\":\"website\",");

        // User data
        sb.append("\"user_data\":{");
        List<String> userFields = new ArrayList<>();
        if (!emailHash.isEmpty())  userFields.add("\"em\":\"" + emailHash + "\"");
        if (!phoneHash.isEmpty())  userFields.add("\"ph\":\"" + phoneHash + "\"");
        if (!prenomHash.isEmpty()) userFields.add("\"fn\":\"" + prenomHash + "\"");
        if (!nomHash.isEmpty())    userFields.add("\"ln\":\"" + nomHash + "\"");
        if (ipClient != null && !ipClient.isBlank())  userFields.add("\"client_ip_address\":\"" + jsonEscape(ipClient) + "\"");
        if (userAgent != null && !userAgent.isBlank()) userFields.add("\"client_user_agent\":\"" + jsonEscape(userAgent) + "\"");
        if (dto.getFbp() != null && !dto.getFbp().isBlank()) userFields.add("\"fbp\":\"" + jsonEscape(dto.getFbp()) + "\"");
        if (dto.getFbc() != null && !dto.getFbc().isBlank()) userFields.add("\"fbc\":\"" + jsonEscape(dto.getFbc()) + "\"");
        sb.append(String.join(",", userFields));
        sb.append("},");

        // Custom data
        sb.append("\"custom_data\":{");
        sb.append("\"value\":").append(commande.getMontantTotal()).append(",");
        sb.append("\"currency\":\"XOF\",");
        sb.append("\"content_type\":\"product\",");
        sb.append("\"num_items\":").append(numItems).append(",");
        sb.append("\"content_ids\":[");
        sb.append(contentIds.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(",")));
        sb.append("]}");

        sb.append("}]");

        if (testCode != null) {
            sb.append(",\"test_event_code\":\"").append(jsonEscape(testCode)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    // ── Résolution des données client ─────────────────────────────────────────

    private String resolveEmail(Commande c, CommandeDTO dto) {
        if (c.getClient() != null && c.getClient().getEmail() != null) return c.getClient().getEmail();
        return dto.getEmailInvite() != null ? dto.getEmailInvite() : "";
    }

    private String resolvePhone(Commande c, CommandeDTO dto) {
        if (c.getClient() != null && c.getClient().getTelephone() != null) return c.getClient().getTelephone();
        return dto.getTelephoneInvite() != null ? dto.getTelephoneInvite() : "";
    }

    private String resolvePrenom(Commande c, CommandeDTO dto) {
        if (c.getClient() != null && c.getClient().getPrenom() != null) return c.getClient().getPrenom();
        return dto.getPrenomInvite() != null ? dto.getPrenomInvite() : "";
    }

    private String resolveNom(Commande c, CommandeDTO dto) {
        if (c.getClient() != null && c.getClient().getNom() != null) return c.getClient().getNom();
        return dto.getNomInvite() != null ? dto.getNomInvite() : "";
    }

    // ── Normalisation + hachage SHA-256 (spec Meta) ───────────────────────────

    /** Email en minuscules, sans espaces. */
    static String normalizeEmail(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase();
    }

    /** Téléphone : chiffres uniquement, sans indicatif si on peut l'inférer. */
    static String normalizePhone(String s) {
        if (s == null) return "";
        String digits = s.replaceAll("[^0-9]", "");
        // Ajouter l'indicatif Sénégal (221) si le numéro commence par 7 ou 3 (numéro local)
        if (digits.length() == 9 && (digits.startsWith("7") || digits.startsWith("3"))) {
            digits = "221" + digits;
        }
        return digits;
    }

    /** Texte générique : minuscules, trim. */
    static String normalizeText(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase();
    }

    static String hashSha256(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return String.format("%064x", new BigInteger(1, hash));
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
