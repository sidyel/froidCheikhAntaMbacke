package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.LivraisonDTO;
import com.froidcheikh.ecommerce.dto.ZoneLivraisonDTO;
import com.froidcheikh.ecommerce.entity.ZoneLivraison;
import com.froidcheikh.ecommerce.exception.ResourceNotFoundException;
import com.froidcheikh.ecommerce.repository.ZoneLivraisonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ZoneLivraisonService {

    private final ZoneLivraisonRepository zoneRepository;

    /** Cache temporaire des prévisualisations CSV en attente de confirmation. */
    private final Map<String, List<LivraisonDTO.CsvPreviewResponse.CsvLigne>> pendingImports =
            new ConcurrentHashMap<>();

    // ─── Lecture publique ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ZoneLivraisonDTO.PublicDTO> getZonesActives() {
        return zoneRepository.findByActifTrueOrderByVilleLabelAscOrdreAsc()
                .stream()
                .map(this::toPublicDTO)
                .toList();
    }

    // ─── CRUD admin ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ZoneLivraisonDTO.AdminDTO> getAllZones() {
        return zoneRepository.findAllByOrderByVilleLabelAscOrdreAsc()
                .stream()
                .map(this::toAdminDTO)
                .toList();
    }

    public ZoneLivraisonDTO.AdminDTO creer(ZoneLivraisonDTO.CreateDTO dto) {
        if (zoneRepository.existsByVilleIgnoreCaseAndQuartierIgnoreCase(dto.getVille(), dto.getQuartier())) {
            throw new IllegalArgumentException(
                    "Une zone existe déjà pour : " + dto.getVilleLabel() + " / " + dto.getQuartierLabel());
        }
        ZoneLivraison zone = fromCreateDTO(dto);
        return toAdminDTO(zoneRepository.save(zone));
    }

    public ZoneLivraisonDTO.AdminDTO modifier(Long id, ZoneLivraisonDTO.CreateDTO dto) {
        ZoneLivraison zone = findById(id);
        // Vérifier unicité seulement si (ville, quartier) a changé
        boolean cleChangee = !zone.getVille().equalsIgnoreCase(dto.getVille())
                || !zone.getQuartier().equalsIgnoreCase(dto.getQuartier());
        if (cleChangee && zoneRepository.existsByVilleIgnoreCaseAndQuartierIgnoreCase(
                dto.getVille(), dto.getQuartier())) {
            throw new IllegalArgumentException(
                    "Une zone existe déjà pour : " + dto.getVilleLabel() + " / " + dto.getQuartierLabel());
        }
        appliquerCreateDTO(zone, dto);
        return toAdminDTO(zoneRepository.save(zone));
    }

    /** Désactivation uniquement (jamais de suppression physique d'une zone). */
    public void desactiver(Long id) {
        ZoneLivraison zone = findById(id);
        zone.setActif(false);
        zoneRepository.save(zone);
    }

    public ZoneLivraisonDTO.AdminDTO toggleActif(Long id) {
        ZoneLivraison zone = findById(id);
        zone.setActif(!Boolean.TRUE.equals(zone.getActif()));
        return toAdminDTO(zoneRepository.save(zone));
    }

    // ─── Import CSV ─────────────────────────────────────────────────────────
    // Colonnes : ville,ville_label,quartier,quartier_label,prix_standard,
    //            prix_express,delai_estime,express_disponible,eligible_livraison_gratuite

    public LivraisonDTO.CsvPreviewResponse previsualiserCsv(MultipartFile fichier) {
        List<LivraisonDTO.CsvPreviewResponse.CsvLigne> aCreer = new ArrayList<>();
        List<LivraisonDTO.CsvPreviewResponse.CsvLigne> aModifier = new ArrayList<>();
        List<LivraisonDTO.CsvPreviewResponse.CsvErreur> enErreur = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(fichier.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("Fichier CSV vide");
            }

            String line;
            int num = 1;
            while ((line = reader.readLine()) != null) {
                num++;
                if (line.isBlank()) continue;
                try {
                    LivraisonDTO.CsvPreviewResponse.CsvLigne parsed = parseCsvLine(line, num);
                    boolean exists = zoneRepository.existsByVilleIgnoreCaseAndQuartierIgnoreCase(
                            parsed.getVille(), parsed.getQuartier());
                    if (exists) {
                        aModifier.add(parsed);
                    } else {
                        aCreer.add(parsed);
                    }
                } catch (Exception e) {
                    LivraisonDTO.CsvPreviewResponse.CsvErreur err = new LivraisonDTO.CsvPreviewResponse.CsvErreur();
                    err.setNumeroLigne(num);
                    err.setContenu(line);
                    err.setMotif(e.getMessage());
                    enErreur.add(err);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lecture CSV : " + e.getMessage(), e);
        }

        String token = UUID.randomUUID().toString();
        List<LivraisonDTO.CsvPreviewResponse.CsvLigne> toutesLignes = new ArrayList<>(aCreer);
        toutesLignes.addAll(aModifier);
        pendingImports.put(token, toutesLignes);

        LivraisonDTO.CsvPreviewResponse preview = new LivraisonDTO.CsvPreviewResponse();
        preview.setLignesACreer(aCreer);
        preview.setLignesAModifier(aModifier);
        preview.setLignesEnErreur(enErreur);
        preview.setTotalTraitees(aCreer.size() + aModifier.size());
        preview.setToken(token);
        return preview;
    }

    public int confirmerImportCsv(String token) {
        List<LivraisonDTO.CsvPreviewResponse.CsvLigne> lignes = pendingImports.remove(token);
        if (lignes == null) {
            throw new IllegalArgumentException("Token d'import invalide ou expiré");
        }
        int count = 0;
        for (LivraisonDTO.CsvPreviewResponse.CsvLigne l : lignes) {
            ZoneLivraison zone = zoneRepository
                    .findByVilleIgnoreCaseAndQuartierIgnoreCase(l.getVille(), l.getQuartier())
                    .orElse(new ZoneLivraison());
            zone.setVille(l.getVille());
            zone.setVilleLabel(l.getVilleLabel());
            zone.setQuartier(l.getQuartier());
            zone.setQuartierLabel(l.getQuartierLabel());
            zone.setPrixStandard(l.getPrixStandard());
            zone.setPrixExpress(l.getPrixExpress());
            zone.setDelaiEstime(l.getDelaiEstime());
            zone.setExpressDisponible(l.getExpressDisponible() != null ? l.getExpressDisponible() : Boolean.TRUE);
            zone.setEligibleLivraisonGratuite(l.getEligibleLivraisonGratuite() != null ? l.getEligibleLivraisonGratuite() : Boolean.FALSE);
            if (zone.getActif() == null) zone.setActif(Boolean.TRUE);
            if (zone.getOrdre() == null) zone.setOrdre(0);
            zoneRepository.save(zone);
            count++;
        }
        log.info("Import CSV confirmé : {} zones créées/mises à jour", count);
        return count;
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private ZoneLivraison findById(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone introuvable : " + id));
    }

    private LivraisonDTO.CsvPreviewResponse.CsvLigne parseCsvLine(String line, int num) {
        String[] cols = splitCsvLine(line);
        if (cols.length < 5) {
            throw new IllegalArgumentException("Ligne " + num + " : colonnes insuffisantes (min 5)");
        }
        LivraisonDTO.CsvPreviewResponse.CsvLigne l = new LivraisonDTO.CsvPreviewResponse.CsvLigne();
        l.setNumeroLigne(num);
        l.setVille(clean(cols[0]));
        l.setVilleLabel(clean(cols[1]));
        l.setQuartier(clean(cols[2]));
        l.setQuartierLabel(clean(cols[3]));
        l.setPrixStandard(parseBD(cols[4], "prix_standard", num));
        l.setPrixExpress(cols.length > 5 && !clean(cols[5]).isEmpty() ? parseBD(cols[5], "prix_express", num) : null);
        l.setDelaiEstime(cols.length > 6 ? clean(cols[6]) : null);
        l.setExpressDisponible(cols.length > 7 ? parseBool(cols[7]) : Boolean.TRUE);
        l.setEligibleLivraisonGratuite(cols.length > 8 ? parseBool(cols[8]) : Boolean.FALSE);

        if (l.getVille().isEmpty()) throw new IllegalArgumentException("Ligne " + num + " : ville vide");
        if (l.getQuartier().isEmpty()) throw new IllegalArgumentException("Ligne " + num + " : quartier vide");
        if (l.getPrixStandard().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Ligne " + num + " : prix_standard négatif");
        return l;
    }

    private String[] splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString());
        return result.toArray(new String[0]);
    }

    private String clean(String s) {
        return s == null ? "" : s.trim();
    }

    private BigDecimal parseBD(String s, String col, int num) {
        try {
            return new BigDecimal(clean(s).replace(" ", "").replace(" ", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ligne " + num + " : valeur invalide pour " + col + " : " + s);
        }
    }

    private Boolean parseBool(String s) {
        String v = clean(s).toLowerCase();
        return "1".equals(v) || "true".equals(v) || "oui".equals(v) || "yes".equals(v);
    }

    private ZoneLivraison fromCreateDTO(ZoneLivraisonDTO.CreateDTO dto) {
        ZoneLivraison z = new ZoneLivraison();
        appliquerCreateDTO(z, dto);
        return z;
    }

    private void appliquerCreateDTO(ZoneLivraison z, ZoneLivraisonDTO.CreateDTO dto) {
        z.setVille(dto.getVille());
        z.setVilleLabel(dto.getVilleLabel());
        z.setQuartier(dto.getQuartier());
        z.setQuartierLabel(dto.getQuartierLabel());
        z.setPrixStandard(dto.getPrixStandard());
        z.setPrixExpress(dto.getPrixExpress());
        z.setExpressDisponible(dto.getExpressDisponible() != null ? dto.getExpressDisponible() : Boolean.TRUE);
        z.setDelaiEstime(dto.getDelaiEstime());
        z.setActif(dto.getActif() != null ? dto.getActif() : Boolean.TRUE);
        z.setEligibleLivraisonGratuite(dto.getEligibleLivraisonGratuite() != null ? dto.getEligibleLivraisonGratuite() : Boolean.FALSE);
        z.setOrdre(dto.getOrdre() != null ? dto.getOrdre() : 0);
    }

    private ZoneLivraisonDTO.PublicDTO toPublicDTO(ZoneLivraison z) {
        ZoneLivraisonDTO.PublicDTO dto = new ZoneLivraisonDTO.PublicDTO();
        dto.setId(z.getId());
        dto.setVilleLabel(z.getVilleLabel());
        dto.setQuartierLabel(z.getQuartierLabel());
        dto.setPrixStandard(z.getPrixStandard());
        dto.setPrixExpress(Boolean.TRUE.equals(z.getExpressDisponible()) ? z.getPrixExpress() : null);
        dto.setExpressDisponible(z.getExpressDisponible());
        dto.setDelaiEstime(z.getDelaiEstime());
        dto.setEligibleLivraisonGratuite(z.getEligibleLivraisonGratuite());
        dto.setOrdre(z.getOrdre());
        return dto;
    }

    private ZoneLivraisonDTO.AdminDTO toAdminDTO(ZoneLivraison z) {
        ZoneLivraisonDTO.AdminDTO dto = new ZoneLivraisonDTO.AdminDTO();
        dto.setId(z.getId());
        dto.setVille(z.getVille());
        dto.setVilleLabel(z.getVilleLabel());
        dto.setQuartier(z.getQuartier());
        dto.setQuartierLabel(z.getQuartierLabel());
        dto.setPrixStandard(z.getPrixStandard());
        dto.setPrixExpress(z.getPrixExpress());
        dto.setExpressDisponible(z.getExpressDisponible());
        dto.setDelaiEstime(z.getDelaiEstime());
        dto.setActif(z.getActif());
        dto.setEligibleLivraisonGratuite(z.getEligibleLivraisonGratuite());
        dto.setOrdre(z.getOrdre());
        return dto;
    }
}
