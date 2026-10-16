package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.ParametresSiteDTO;
import com.froidcheikh.ecommerce.entity.ParametresSite;
import com.froidcheikh.ecommerce.entity.HoraireJour;
import com.froidcheikh.ecommerce.entity.TelephoneSite;
import com.froidcheikh.ecommerce.repository.ParametresSiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParametresSiteService {

    private final ParametresSiteRepository repository;

    @Cacheable("siteConfig")
    public ParametresSiteDTO.PublicDTO getPublicConfig() {
        return repository.findAll().stream()
                .findFirst()
                .map(this::toPublicDTO)
                .orElseGet(this::defaultPublicDTO);
    }

    public ParametresSiteDTO.AdminDTO getAdminConfig() {
        return repository.findAll().stream()
                .findFirst()
                .map(this::toAdminDTO)
                .orElseGet(ParametresSiteDTO.AdminDTO::new);
    }

    @Transactional
    @CacheEvict(value = "siteConfig", allEntries = true)
    public ParametresSiteDTO.AdminDTO update(ParametresSiteDTO.AdminDTO dto, String modifiePar) {
        ParametresSite entity = repository.findAll().stream()
                .findFirst()
                .orElseGet(ParametresSite::new);

        applyDTO(entity, dto);
        entity.setModifiePar(modifiePar);
        entity = repository.save(entity);
        return toAdminDTO(entity);
    }

    @Transactional
    @CacheEvict(value = "siteConfig", allEntries = true)
    public String updateCachetUrl(String url) {
        ParametresSite entity = repository.findAll().stream().findFirst().orElseThrow();
        entity.setUrlCachetSignature(url);
        repository.save(entity);
        return url;
    }

    @Transactional
    @CacheEvict(value = "siteConfig", allEntries = true)
    public String updateImageEnTeteUrl(String url) {
        ParametresSite entity = repository.findAll().stream().findFirst().orElseThrow();
        entity.setUrlImageEnTete(url);
        repository.save(entity);
        return url;
    }

    @Transactional
    @CacheEvict(value = "siteConfig", allEntries = true)
    public String updateImagePiedUrl(String url) {
        ParametresSite entity = repository.findAll().stream().findFirst().orElseThrow();
        entity.setUrlImagePied(url);
        repository.save(entity);
        return url;
    }

    @Transactional
    @CacheEvict(value = "siteConfig", allEntries = true)
    public void initDefault(String nomSite, String sloganSite, String emailContact, Integer anneesExperience) {
        if (!repository.findAll().isEmpty()) return;

        ParametresSite p = new ParametresSite();
        p.setNomSite(nomSite);
        p.setSloganSite(sloganSite);
        p.setEmailContact(emailContact);
        p.setAnneesExperience(anneesExperience);
        p.setCouleurDocuments("#000080");
        p.setTexteDescriptifActivites("Réparation et Vente de tous Appareils de Froid : Electricité, Plomberie, Climatisation Centrale, Chambre Froide, Split Système, Climatiseur et Frigos");
        p.setDevise("XOF");
        p.setLibelleDevise("FCFA");
        p.setRaisonSociale("FROID CHEIKH ANTA MBACKE - BABACAR DIONE");
        p.setAdresseSiege("Ouest Foire Cité Aelmas Villa N°168 Rue 577 Yoff, Dakar, Sénégal");
        p.setBoitePostale("B.P. 7605 Dakar");
        p.setRccm("92-A-5673");
        p.setNinea("0151340-2B1");
        p.setBanqueNom("CBAO");
        p.setNumeroCompteBancaire("36017179-49");
        p.getTelephones().add(new com.froidcheikh.ecommerce.entity.TelephoneSite("33 820 16 33", "Fixe", false, true));
        p.getTelephones().add(new com.froidcheikh.ecommerce.entity.TelephoneSite("77 335 20 00", "Mobile", true, true));
        p.getTelephones().add(new com.froidcheikh.ecommerce.entity.TelephoneSite("76 888 04 42", "Mobile", false, false));
        repository.save(p);
    }

    /** Migration douce : renseigne les champs de facturation si absents sur la ligne existante. */
    @org.springframework.transaction.annotation.Transactional
    public void initInfosFacturationSiAbsentes() {
        repository.findAll().stream().findFirst().ifPresent(p -> {
            boolean modifie = false;
            if (p.getRaisonSociale() == null || p.getRaisonSociale().isBlank()) {
                p.setRaisonSociale("FROID CHEIKH ANTA MBACKE - BABACAR DIONE"); modifie = true;
            }
            if (p.getTexteDescriptifActivites() == null || p.getTexteDescriptifActivites().isBlank()) {
                p.setTexteDescriptifActivites("Réparation et Vente de tous Appareils de Froid : Electricité, Plomberie, Climatisation Centrale, Chambre Froide, Split Système, Climatiseur et Frigos"); modifie = true;
            }
            if (p.getAdresseSiege() == null || p.getAdresseSiege().isBlank()) {
                p.setAdresseSiege("Ouest Foire Cité Aelmas Villa N°168 Rue 577 Yoff, Dakar, Sénégal"); modifie = true;
            }
            if (p.getBoitePostale() == null || p.getBoitePostale().isBlank()) {
                p.setBoitePostale("B.P. 7605 Dakar"); modifie = true;
            }
            if (p.getRccm() == null || p.getRccm().isBlank()) {
                p.setRccm("92-A-5673"); modifie = true;
            }
            if (p.getNinea() == null || p.getNinea().isBlank()) {
                p.setNinea("0151340-2B1"); modifie = true;
            }
            if (p.getBanqueNom() == null || p.getBanqueNom().isBlank()) {
                p.setBanqueNom("CBAO"); modifie = true;
            }
            if (p.getNumeroCompteBancaire() == null || p.getNumeroCompteBancaire().isBlank()) {
                p.setNumeroCompteBancaire("36017179-49"); modifie = true;
            }
            if (modifie) repository.save(p);
        });
    }

    /** Migration douce : ajoute les téléphones par défaut si la liste est vide. */
    @org.springframework.transaction.annotation.Transactional
    public void initTelephonesSiAbsents() {
        repository.findAll().stream().findFirst().ifPresent(p -> {
            if (p.getTelephones() == null || p.getTelephones().isEmpty()) {
                p.getTelephones().add(new com.froidcheikh.ecommerce.entity.TelephoneSite("33 820 16 33", "Fixe", false, true));
                p.getTelephones().add(new com.froidcheikh.ecommerce.entity.TelephoneSite("77 335 20 00", "Mobile", true, true));
                p.getTelephones().add(new com.froidcheikh.ecommerce.entity.TelephoneSite("76 888 04 42", "Mobile", false, false));
                repository.save(p);
            }
        });
    }

    /** Migration douce : remet devise à son code ISO si une ancienne migration l'avait mis à "FCFA". */
    @org.springframework.transaction.annotation.Transactional
    public void migrerDevise(String ancienne, String nouvelle) {
        repository.findAll().stream().findFirst().ifPresent(p -> {
            if (ancienne.equals(p.getDevise())) {
                p.setDevise(nouvelle);
                repository.save(p);
            }
        });
    }

    /**
     * Migration ponctuelle idempotente : corrige texteDescriptifActivites seulement si sa valeur
     * est EXACTEMENT l'ancienne valeur connue. Sans effet si le champ a été modifié depuis l'admin.
     */
    @org.springframework.transaction.annotation.Transactional
    public void migrerTexteDescriptifActivites(String ancienne, String nouvelle) {
        repository.findAll().stream().findFirst().ifPresent(p -> {
            if (ancienne.equals(p.getTexteDescriptifActivites())) {
                p.setTexteDescriptifActivites(nouvelle);
                repository.save(p);
            }
        });
    }

    /** Migration ponctuelle idempotente : corrige boitePostale si la valeur est exactement l'ancienne. */
    @org.springframework.transaction.annotation.Transactional
    public void migrerBoitePostale(String ancienne, String nouvelle) {
        repository.findAll().stream().findFirst().ifPresent(p -> {
            if (ancienne.equals(p.getBoitePostale())) {
                p.setBoitePostale(nouvelle);
                repository.save(p);
            }
        });
    }

    /**
     * Migration ponctuelle idempotente : supprime le numéro de test 77 330 00 06 et installe
     * les trois numéros réels. N'agit que si 77 330 00 06 est présent dans la liste.
     */
    @org.springframework.transaction.annotation.Transactional
    public void migrerTelephones() {
        repository.findAll().stream().findFirst().ifPresent(p -> {
            boolean aNumeroTest = p.getTelephones().stream()
                    .anyMatch(t -> "77 330 00 06".equals(t.getNumero()));
            if (!aNumeroTest) return;

            p.getTelephones().removeIf(t -> "77 330 00 06".equals(t.getNumero()));

            java.util.Set<String> existants = p.getTelephones().stream()
                    .map(com.froidcheikh.ecommerce.entity.TelephoneSite::getNumero)
                    .collect(java.util.stream.Collectors.toSet());

            if (!existants.contains("33 820 16 33"))
                p.getTelephones().add(new com.froidcheikh.ecommerce.entity.TelephoneSite("33 820 16 33", "Fixe", false, true));
            if (!existants.contains("77 335 20 00"))
                p.getTelephones().add(new com.froidcheikh.ecommerce.entity.TelephoneSite("77 335 20 00", "Mobile", true, true));
            if (!existants.contains("76 888 04 42"))
                p.getTelephones().add(new com.froidcheikh.ecommerce.entity.TelephoneSite("76 888 04 42", "Mobile", false, false));

            repository.save(p);
        });
    }

    /** Migration douce : renseigne libelleDevise si null sur la ligne existante. */
    @org.springframework.transaction.annotation.Transactional
    public void initLibelleDeviseSiAbsent(String libelle) {
        repository.findAll().stream().findFirst().ifPresent(p -> {
            if (p.getLibelleDevise() == null || p.getLibelleDevise().isBlank()) {
                p.setLibelleDevise(libelle);
                repository.save(p);
            }
        });
    }

    /**
     * Migration douce : renseigne couleurDocuments si null sur la ligne existante.
     * Appelé par DataInitializer au démarrage pour les bases déjà initialisées.
     */
    @org.springframework.transaction.annotation.Transactional
    public void initCouleurDocumentsSiAbsente(String couleur) {
        repository.findAll().stream().findFirst().ifPresent(p -> {
            if (p.getCouleurDocuments() == null) {
                p.setCouleurDocuments(couleur);
                repository.save(p);
            }
        });
    }

    /**
     * Migration ponctuelle idempotente (oct. 2026) : vide conditionsPaiementDefaut
     * et mentionExonerationTva pour que ces mentions n'apparaissent plus dans les PDFs.
     * Conditions : conditionsPaiementDefaut vidé si non-null ; mentionExonerationTva
     * vidé si encore à la valeur initiale "TVA non applicable".
     */
    @org.springframework.transaction.annotation.Transactional
    public void viderMentionsDefaut() {
        repository.findAll().stream().findFirst().ifPresent(p -> {
            boolean modifie = false;
            if (p.getConditionsPaiementDefaut() != null && !p.getConditionsPaiementDefaut().isBlank()) {
                p.setConditionsPaiementDefaut(null);
                modifie = true;
            }
            if ("TVA non applicable".equals(p.getMentionExonerationTva())) {
                p.setMentionExonerationTva(null);
                modifie = true;
            }
            if (modifie) repository.save(p);
        });
    }

    public boolean exists() {
        return !repository.findAll().isEmpty();
    }

    @Cacheable(value = "siteConfig", key = "'zoneId'")
    public ZoneId getSiteZoneId() {
        return repository.findAll().stream().findFirst()
                .map(p -> {
                    String tz = p.getFuseauHoraire();
                    if (tz == null || tz.isBlank()) return ZoneId.of("Africa/Dakar");
                    try { return ZoneId.of(tz); }
                    catch (Exception e) { return ZoneId.of("Africa/Dakar"); }
                })
                .orElse(ZoneId.of("Africa/Dakar"));
    }

    // ---- mapping helpers ----

    private ParametresSiteDTO.PublicDTO toPublicDTO(ParametresSite p) {
        ParametresSiteDTO.PublicDTO dto = new ParametresSiteDTO.PublicDTO();
        dto.setNomSite(p.getNomSite());
        dto.setSloganSite(p.getSloganSite());
        dto.setDescriptionSite(p.getDescriptionSite());
        dto.setLogoUrl(p.getLogoUrl());
        dto.setFaviconUrl(p.getFaviconUrl());
        dto.setEmailContact(p.getEmailContact());
        dto.setTelephones(mapTelephones(p.getTelephones()));
        dto.setAdresseComplete(p.getAdresseComplete());
        dto.setVille(p.getVille());
        dto.setPays(p.getPays());
        dto.setFacebookUrl(p.getFacebookUrl());
        dto.setInstagramUrl(p.getInstagramUrl());
        dto.setTwitterUrl(p.getTwitterUrl());
        dto.setLinkedinUrl(p.getLinkedinUrl());
        dto.setYoutubeUrl(p.getYoutubeUrl());
        dto.setTiktokUrl(p.getTiktokUrl());
        dto.setCouleurPrimaire(p.getCouleurPrimaire());
        dto.setCouleurSecondaire(p.getCouleurSecondaire());
        dto.setCouleurAccent(p.getCouleurAccent());
        dto.setPolice(p.getPolice());
        dto.setDevise(p.getDevise());
        dto.setSeuilLivraisonGratuite(p.getSeuilLivraisonGratuite());
        dto.setMetaTitle(p.getMetaTitle());
        dto.setMetaDescription(p.getMetaDescription());
        dto.setMetaKeywords(p.getMetaKeywords());
        dto.setMission(p.getMission());
        dto.setVision(p.getVision());
        dto.setAnneesExperience(p.getAnneesExperience());
        dto.setDescriptionLivraison(p.getDescriptionLivraison());
        dto.setAdresseCourte(p.getAdresseCourte());
        dto.setHorairesOuverture(mapHoraires(p.getHorairesOuverture()));
        dto.setAfficherHeroSlider(p.getAfficherHeroSlider());
        dto.setAfficherPartenaires(p.getAfficherPartenaires());
        dto.setAfficherPrestations(p.getAfficherPrestations());
        dto.setAfficherEquipe(p.getAfficherEquipe());
        dto.setAfficherRealisations(p.getAfficherRealisations());
        // WhatsApp : numéro de la boutique calculé + templates
        dto.setIndicatifPaysDefaut(p.getIndicatifPaysDefaut());
        dto.setWhatsappMsgNouvelleCommande(p.getWhatsappMsgNouvelleCommande());
        dto.setWhatsappMsgConfirmee(p.getWhatsappMsgConfirmee());
        dto.setWhatsappMsgLivree(p.getWhatsappMsgLivree());
        dto.setWhatsappMsgAnnulee(p.getWhatsappMsgAnnulee());
        p.getTelephones().stream()
                .filter(t -> Boolean.TRUE.equals(t.getWhatsapp()))
                .findFirst()
                .ifPresent(t -> dto.setTelephoneWhatsappBoutique(t.getNumero()));
        return dto;
    }

    private ParametresSiteDTO.AdminDTO toAdminDTO(ParametresSite p) {
        ParametresSiteDTO.AdminDTO dto = new ParametresSiteDTO.AdminDTO();
        dto.setNomSite(p.getNomSite());
        dto.setSloganSite(p.getSloganSite());
        dto.setDescriptionSite(p.getDescriptionSite());
        dto.setLogoUrl(p.getLogoUrl());
        dto.setFaviconUrl(p.getFaviconUrl());
        dto.setEmailContact(p.getEmailContact());
        dto.setEmailSupport(p.getEmailSupport());
        dto.setTelephones(mapTelephones(p.getTelephones()));
        dto.setAdresseComplete(p.getAdresseComplete());
        dto.setVille(p.getVille());
        dto.setPays(p.getPays());
        dto.setFacebookUrl(p.getFacebookUrl());
        dto.setInstagramUrl(p.getInstagramUrl());
        dto.setTwitterUrl(p.getTwitterUrl());
        dto.setLinkedinUrl(p.getLinkedinUrl());
        dto.setYoutubeUrl(p.getYoutubeUrl());
        dto.setTiktokUrl(p.getTiktokUrl());
        dto.setCouleurPrimaire(p.getCouleurPrimaire());
        dto.setCouleurSecondaire(p.getCouleurSecondaire());
        dto.setCouleurAccent(p.getCouleurAccent());
        dto.setPolice(p.getPolice());
        dto.setMentionsLegales(p.getMentionsLegales());
        dto.setPolitiqueConfidentialite(p.getPolitiqueConfidentialite());
        dto.setCgv(p.getCgv());
        dto.setRaisonSociale(p.getRaisonSociale());
        dto.setFormeJuridique(p.getFormeJuridique());
        dto.setNinea(p.getNinea());
        dto.setRccm(p.getRccm());
        dto.setNumeroCfe(p.getNumeroCfe());
        dto.setCapitalSocial(p.getCapitalSocial());
        dto.setDevise(p.getDevise());
        dto.setLibelleDevise(p.getLibelleDevise());
        dto.setSeuilLivraisonGratuite(p.getSeuilLivraisonGratuite());
        dto.setMetaTitle(p.getMetaTitle());
        dto.setMetaDescription(p.getMetaDescription());
        dto.setMetaKeywords(p.getMetaKeywords());
        dto.setDateModification(p.getDateModification());
        dto.setModifiePar(p.getModifiePar());
        dto.setMission(p.getMission());
        dto.setVision(p.getVision());
        dto.setAnneesExperience(p.getAnneesExperience());
        dto.setDescriptionLivraison(p.getDescriptionLivraison());
        dto.setAdresseCourte(p.getAdresseCourte());
        dto.setHorairesOuverture(mapHoraires(p.getHorairesOuverture()));
        dto.setAfficherHeroSlider(p.getAfficherHeroSlider());
        dto.setAfficherPartenaires(p.getAfficherPartenaires());
        dto.setAfficherPrestations(p.getAfficherPrestations());
        dto.setAfficherEquipe(p.getAfficherEquipe());
        dto.setAfficherRealisations(p.getAfficherRealisations());
        // WhatsApp
        dto.setIndicatifPaysDefaut(p.getIndicatifPaysDefaut());
        dto.setWhatsappMsgNouvelleCommande(p.getWhatsappMsgNouvelleCommande());
        dto.setWhatsappMsgConfirmee(p.getWhatsappMsgConfirmee());
        dto.setWhatsappMsgLivree(p.getWhatsappMsgLivree());
        dto.setWhatsappMsgAnnulee(p.getWhatsappMsgAnnulee());
        // En-tête documents PDF
        dto.setCouleurDocuments(p.getCouleurDocuments());
        dto.setTexteDescriptifActivites(p.getTexteDescriptifActivites());
        dto.setBoitePostale(p.getBoitePostale());
        dto.setBanqueNom(p.getBanqueNom());
        dto.setNumeroCompteBancaire(p.getNumeroCompteBancaire());
        dto.setModeEnTeteDocuments(p.getModeEnTeteDocuments());
        dto.setUrlImageEnTete(p.getUrlImageEnTete());
        dto.setUrlImagePied(p.getUrlImagePied());
        return dto;
    }

    private void applyDTO(ParametresSite p, ParametresSiteDTO.AdminDTO dto) {
        if (dto.getNomSite() != null) p.setNomSite(dto.getNomSite());
        if (dto.getSloganSite() != null) p.setSloganSite(dto.getSloganSite());
        if (dto.getDescriptionSite() != null) p.setDescriptionSite(dto.getDescriptionSite());
        if (dto.getLogoUrl() != null) p.setLogoUrl(dto.getLogoUrl());
        if (dto.getFaviconUrl() != null) p.setFaviconUrl(dto.getFaviconUrl());
        if (dto.getEmailContact() != null) p.setEmailContact(dto.getEmailContact());
        if (dto.getEmailSupport() != null) p.setEmailSupport(dto.getEmailSupport());
        if (dto.getTelephones() != null) {
            p.getTelephones().clear();
            dto.getTelephones().forEach(t -> {
                TelephoneSite tel = new TelephoneSite();
                tel.setNumero(t.getNumero());
                tel.setLibelle(t.getLibelle());
                tel.setWhatsapp(Boolean.TRUE.equals(t.getWhatsapp()));
                tel.setAfficherEnDocuments(t.getAfficherEnDocuments() == null || t.getAfficherEnDocuments());
                p.getTelephones().add(tel);
            });
        }
        if (dto.getAdresseComplete() != null) p.setAdresseComplete(dto.getAdresseComplete());
        if (dto.getVille() != null) p.setVille(dto.getVille());
        if (dto.getPays() != null) p.setPays(dto.getPays());
        if (dto.getFacebookUrl() != null) p.setFacebookUrl(dto.getFacebookUrl());
        if (dto.getInstagramUrl() != null) p.setInstagramUrl(dto.getInstagramUrl());
        if (dto.getTwitterUrl() != null) p.setTwitterUrl(dto.getTwitterUrl());
        if (dto.getLinkedinUrl() != null) p.setLinkedinUrl(dto.getLinkedinUrl());
        if (dto.getYoutubeUrl() != null) p.setYoutubeUrl(dto.getYoutubeUrl());
        if (dto.getTiktokUrl() != null) p.setTiktokUrl(dto.getTiktokUrl());
        if (dto.getCouleurPrimaire() != null) p.setCouleurPrimaire(dto.getCouleurPrimaire());
        if (dto.getCouleurSecondaire() != null) p.setCouleurSecondaire(dto.getCouleurSecondaire());
        if (dto.getCouleurAccent() != null) p.setCouleurAccent(dto.getCouleurAccent());
        if (dto.getPolice() != null) p.setPolice(dto.getPolice());
        if (dto.getMentionsLegales() != null) p.setMentionsLegales(dto.getMentionsLegales());
        if (dto.getPolitiqueConfidentialite() != null) p.setPolitiqueConfidentialite(dto.getPolitiqueConfidentialite());
        if (dto.getCgv() != null) p.setCgv(dto.getCgv());
        if (dto.getRaisonSociale() != null) p.setRaisonSociale(dto.getRaisonSociale());
        if (dto.getFormeJuridique() != null) p.setFormeJuridique(dto.getFormeJuridique());
        if (dto.getNinea() != null) p.setNinea(dto.getNinea());
        if (dto.getRccm() != null) p.setRccm(dto.getRccm());
        if (dto.getNumeroCfe() != null) p.setNumeroCfe(dto.getNumeroCfe());
        if (dto.getCapitalSocial() != null) p.setCapitalSocial(dto.getCapitalSocial());
        if (dto.getDevise() != null) p.setDevise(dto.getDevise());
        if (dto.getLibelleDevise() != null) p.setLibelleDevise(dto.getLibelleDevise());
        if (dto.getSeuilLivraisonGratuite() != null) p.setSeuilLivraisonGratuite(dto.getSeuilLivraisonGratuite());
        if (dto.getMetaTitle() != null) p.setMetaTitle(dto.getMetaTitle());
        if (dto.getMetaDescription() != null) p.setMetaDescription(dto.getMetaDescription());
        if (dto.getMetaKeywords() != null) p.setMetaKeywords(dto.getMetaKeywords());
        if (dto.getMission() != null) p.setMission(dto.getMission());
        if (dto.getVision() != null) p.setVision(dto.getVision());
        if (dto.getAnneesExperience() != null) p.setAnneesExperience(dto.getAnneesExperience());
        if (dto.getDescriptionLivraison() != null) p.setDescriptionLivraison(dto.getDescriptionLivraison());
        if (dto.getAdresseCourte() != null) p.setAdresseCourte(dto.getAdresseCourte());
        if (dto.getAfficherHeroSlider() != null) p.setAfficherHeroSlider(dto.getAfficherHeroSlider());
        if (dto.getAfficherPartenaires() != null) p.setAfficherPartenaires(dto.getAfficherPartenaires());
        if (dto.getAfficherPrestations() != null) p.setAfficherPrestations(dto.getAfficherPrestations());
        if (dto.getAfficherEquipe() != null) p.setAfficherEquipe(dto.getAfficherEquipe());
        if (dto.getAfficherRealisations() != null) p.setAfficherRealisations(dto.getAfficherRealisations());
        // WhatsApp
        if (dto.getIndicatifPaysDefaut() != null) p.setIndicatifPaysDefaut(dto.getIndicatifPaysDefaut());
        if (dto.getWhatsappMsgNouvelleCommande() != null) p.setWhatsappMsgNouvelleCommande(dto.getWhatsappMsgNouvelleCommande());
        if (dto.getWhatsappMsgConfirmee() != null) p.setWhatsappMsgConfirmee(dto.getWhatsappMsgConfirmee());
        if (dto.getWhatsappMsgLivree() != null) p.setWhatsappMsgLivree(dto.getWhatsappMsgLivree());
        if (dto.getWhatsappMsgAnnulee() != null) p.setWhatsappMsgAnnulee(dto.getWhatsappMsgAnnulee());
        // En-tête documents PDF
        if (dto.getCouleurDocuments() != null) p.setCouleurDocuments(dto.getCouleurDocuments());
        if (dto.getTexteDescriptifActivites() != null) p.setTexteDescriptifActivites(dto.getTexteDescriptifActivites());
        if (dto.getBoitePostale() != null) p.setBoitePostale(dto.getBoitePostale());
        if (dto.getBanqueNom() != null) p.setBanqueNom(dto.getBanqueNom());
        if (dto.getNumeroCompteBancaire() != null) p.setNumeroCompteBancaire(dto.getNumeroCompteBancaire());
        if (dto.getModeEnTeteDocuments() != null) p.setModeEnTeteDocuments(dto.getModeEnTeteDocuments());
        if (dto.getUrlImageEnTete() != null) p.setUrlImageEnTete(dto.getUrlImageEnTete());
        if (dto.getUrlImagePied() != null) p.setUrlImagePied(dto.getUrlImagePied());
        if (dto.getHorairesOuverture() != null) {
            p.getHorairesOuverture().clear();
            dto.getHorairesOuverture().forEach(h -> {
                HoraireJour horaire = new HoraireJour();
                horaire.setJour(h.getJour());
                horaire.setHeureDebut(h.getHeureDebut());
                horaire.setHeureFin(h.getHeureFin());
                horaire.setFerme(Boolean.TRUE.equals(h.getFerme()));
                p.getHorairesOuverture().add(horaire);
            });
        }
    }

    private List<ParametresSiteDTO.TelephoneSiteDTO> mapTelephones(List<TelephoneSite> telephones) {
        if (telephones == null) return List.of();
        return telephones.stream().map(t -> {
            ParametresSiteDTO.TelephoneSiteDTO dto = new ParametresSiteDTO.TelephoneSiteDTO();
            dto.setNumero(t.getNumero());
            dto.setLibelle(t.getLibelle());
            dto.setWhatsapp(t.getWhatsapp());
            dto.setAfficherEnDocuments(t.getAfficherEnDocuments());
            return dto;
        }).collect(Collectors.toList());
    }

    private List<ParametresSiteDTO.HoraireJourDTO> mapHoraires(List<HoraireJour> horaires) {
        if (horaires == null) return List.of();
        return horaires.stream().map(h -> {
            ParametresSiteDTO.HoraireJourDTO dto = new ParametresSiteDTO.HoraireJourDTO();
            dto.setJour(h.getJour());
            dto.setHeureDebut(h.getHeureDebut());
            dto.setHeureFin(h.getHeureFin());
            dto.setFerme(h.getFerme());
            return dto;
        }).collect(Collectors.toList());
    }

    private ParametresSiteDTO.PublicDTO defaultPublicDTO() {
        ParametresSiteDTO.PublicDTO dto = new ParametresSiteDTO.PublicDTO();
        dto.setCouleurPrimaire("#2563eb");
        dto.setCouleurSecondaire("#16a34a");
        dto.setCouleurAccent("#dc2626");
        dto.setPolice("Inter");
        dto.setDevise("XOF");
        return dto;
    }
}
