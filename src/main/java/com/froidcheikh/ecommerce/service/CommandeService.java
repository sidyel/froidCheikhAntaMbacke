package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.dto.CommandeDTO;
import com.froidcheikh.ecommerce.entity.*;
import com.froidcheikh.ecommerce.exception.ResourceNotFoundException;
import com.froidcheikh.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final ClientRepository clientRepository;
    private final ProduitRepository produitRepository;
    private final PaiementRepository paiementRepository;
    private final ProduitService produitService;
    private final ModelMapper modelMapper;

    public CommandeDTO getCommandeById(Long id) {
        Commande commande = commandeRepository.findByIdWithLignes(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec l'ID : " + id));
        return convertToDTO(commande);
    }

    public CommandeDTO getCommandeByNumero(String numeroCommande) {
        Commande commande = commandeRepository.findByNumeroCommande(numeroCommande)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec le numéro : " + numeroCommande));
        return convertToDTO(commande);
    }

    public Page<CommandeDTO> getCommandesClient(Long clientId, Pageable pageable) {
        return commandeRepository.findByClientIdClient(clientId, pageable)
                .map(this::convertToDTO);
    }

    public CommandeDTO creerCommandeClient(Long clientId, CommandeDTO commandeDTO) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID : " + clientId));

        return creerCommande(commandeDTO, client, null);
    }

    public CommandeDTO creerCommandeInvite(CommandeDTO commandeDTO) {
        log.info("🔄 === DÉBUT CRÉATION COMMANDE INVITÉ - SERVICE ===");
        log.info("📧 Email reçu: {}", commandeDTO.getEmailInvite());
        log.info("💰 Montant DTO reçu: {}", commandeDTO.getMontantTotal());

        return creerCommande(commandeDTO, null, commandeDTO);
    }

    private CommandeDTO creerCommande(CommandeDTO commandeDTO, Client client, CommandeDTO inviteInfo) {
        try {
            log.info("🔧 === DÉBUT CRÉATION COMMANDE DANS creerCommande ===");

            // ✅ ÉTAPE 1 : Vérifier la disponibilité des produits
            for (CommandeDTO.LigneCommandeDTO ligneDTO : commandeDTO.getLignesCommande()) {
                if (!produitService.verifierStockDisponible(ligneDTO.getProduitId(), ligneDTO.getQuantite())) {
                    throw new RuntimeException("Stock insuffisant pour le produit ID : " + ligneDTO.getProduitId());
                }
            }

            // ✅ ÉTAPE 2 : Créer l'entité Commande
            Commande commande = new Commande();
            commande.setNumeroCommande(genererNumeroCommande());
            commande.setClient(client);
            commande.setStatutCommande(Commande.StatutCommande.EN_ATTENTE);
            commande.setModeLivraison(commandeDTO.getModeLivraison());
            commande.setCommentaire(commandeDTO.getCommentaire());

            log.info("📝 Commande de base créée - Numéro: {}", commande.getNumeroCommande());

            // ✅ ÉTAPE 3 : Données invité si applicable
            if (inviteInfo != null) {
                commande.setEmailInvite(inviteInfo.getEmailInvite());
                commande.setNomInvite(inviteInfo.getNomInvite());
                commande.setPrenomInvite(inviteInfo.getPrenomInvite());
                commande.setTelephoneInvite(inviteInfo.getTelephoneInvite());
                log.info("👤 Données invité assignées");
            }

            // ✅ ÉTAPE 4 : Adresse de livraison
            if (commandeDTO.getAdresseLivraison() != null) {
                Commande.AdresseLivraison adresseLivraison = new Commande.AdresseLivraison();
                adresseLivraison.setNom(commandeDTO.getAdresseLivraison().getNom());
                adresseLivraison.setPrenom(commandeDTO.getAdresseLivraison().getPrenom());
                adresseLivraison.setLigne1(commandeDTO.getAdresseLivraison().getLigne1());
                adresseLivraison.setLigne2(commandeDTO.getAdresseLivraison().getLigne2());
                adresseLivraison.setVille(commandeDTO.getAdresseLivraison().getVille());
                adresseLivraison.setCodePostal(commandeDTO.getAdresseLivraison().getCodePostal());
                adresseLivraison.setTelephone(commandeDTO.getAdresseLivraison().getTelephone());
                commande.setAdresseLivraison(adresseLivraison);
                log.info("🏠 Adresse de livraison assignée");
            }

            // ✅ ÉTAPE 5 : Calculer les frais de livraison
            BigDecimal fraisLivraison = calculerFraisLivraison(commandeDTO);
            commande.setFraisLivraison(fraisLivraison);
            log.info("🚚 Frais de livraison calculés: {}", fraisLivraison);

            // ✅ ÉTAPE 6 : CALCULER LE MONTANT TOTAL AVANT LA SAUVEGARDE
            BigDecimal montantTotal = BigDecimal.ZERO;

            // Calculer le montant des lignes de commande
            for (CommandeDTO.LigneCommandeDTO ligneDTO : commandeDTO.getLignesCommande()) {
                Produit produit = produitRepository.findById(ligneDTO.getProduitId())
                        .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé avec l'ID : " + ligneDTO.getProduitId()));

                BigDecimal sousTotal = produit.getPrix().multiply(BigDecimal.valueOf(ligneDTO.getQuantite()));
                montantTotal = montantTotal.add(sousTotal);
                log.info("📦 Produit ID {} - Prix: {} x Qté: {} = Sous-total: {}",
                        ligneDTO.getProduitId(), produit.getPrix(), ligneDTO.getQuantite(), sousTotal);
            }

            // Ajouter les frais de livraison au montant total
            montantTotal = montantTotal.add(fraisLivraison);

            // ✅ ASSIGNER LE MONTANT TOTAL AVANT LA SAUVEGARDE
            commande.setMontantTotal(montantTotal);
            log.info("💰 Montant total calculé et assigné: {}", montantTotal);

            // ✅ ÉTAPE 7 : SAUVEGARDER LA COMMANDE AVEC LE MONTANT TOTAL
            log.info("💾 Sauvegarde de la commande...");
            commande = commandeRepository.save(commande);
            log.info("✅ Commande sauvegardée avec ID: {}", commande.getIdCommande());

            // ✅ ÉTAPE 8 : Créer les lignes de commande
            log.info("📋 Création des lignes de commande...");
            List<LigneCommande> lignesCommande = new ArrayList<>();

            for (CommandeDTO.LigneCommandeDTO ligneDTO : commandeDTO.getLignesCommande()) {
                Produit produit = produitRepository.findById(ligneDTO.getProduitId())
                        .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé avec l'ID : " + ligneDTO.getProduitId()));

                LigneCommande ligne = new LigneCommande();
                ligne.setCommande(commande);
                ligne.setProduit(produit);
                ligne.setQuantite(ligneDTO.getQuantite());
                ligne.setPrixUnitaire(produit.getPrix());
                ligne.setSousTotal(produit.getPrix().multiply(BigDecimal.valueOf(ligneDTO.getQuantite())));
                ligne.setNomProduitCommande(produit.getNomProduit());
                ligne.setRefProduitCommande(produit.getRefProduit());

                lignesCommande.add(ligne);

                // Décrémenter le stock
                produitService.updateStock(produit.getIdProduit(),
                        produit.getStockDisponible() - ligneDTO.getQuantite());

                log.info("📦 Ligne de commande créée pour produit: {}", produit.getNomProduit());
            }

            commande.setLignesCommande(lignesCommande);

            // ✅ ÉTAPE 9 : Sauvegarder la commande finale
            commande = commandeRepository.save(commande);

            log.info("🎉 === COMMANDE CRÉÉE AVEC SUCCÈS ===");
            log.info("🔢 ID: {}", commande.getIdCommande());
            log.info("📄 Numéro: {}", commande.getNumeroCommande());
            log.info("💰 Montant total: {}", commande.getMontantTotal());
            log.info("📦 Nombre de lignes: {}", commande.getLignesCommande().size());

            return convertToDTO(commande);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de la commande: ", e);
            throw new RuntimeException("Erreur lors de la création de la commande: " + e.getMessage(), e);
        }
    }

    public CommandeDTO confirmerPaiement(Long commandeId, CommandeDTO.PaiementDTO paiementDTO) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec l'ID : " + commandeId));

        // Créer le paiement
        Paiement paiement = new Paiement();
        paiement.setCommande(commande);
        paiement.setMethodePaiement(Paiement.MethodePaiement.valueOf(paiementDTO.getMethodePaiement()));
        paiement.setMontant(commande.getMontantTotal());
        paiement.setStatutPaiement(Paiement.StatutPaiement.CONFIRME);
        paiement.setReferencePaiement(genererReferencePaiement());
        paiement.setReferenceExterne(paiementDTO.getReferenceExterne());

        paiement = paiementRepository.save(paiement);

        // Mettre à jour le statut de la commande
        commande.setStatutCommande(Commande.StatutCommande.PAYEE);
        commande = commandeRepository.save(commande);

        log.info("Paiement confirmé pour la commande : {}", commandeId);
        return convertToDTO(commande);
    }

    // Méthodes utilitaires
    private BigDecimal calculerFraisLivraison(CommandeDTO commandeDTO) {
        if (commandeDTO.getModeLivraison() == Commande.ModeLivraison.RETRAIT_MAGASIN) {
            return BigDecimal.ZERO;
        } else if (commandeDTO.getModeLivraison() == Commande.ModeLivraison.LIVRAISON_EXPRESS) {
            return BigDecimal.valueOf(5000); // 5000 FCFA
        } else {
            return BigDecimal.valueOf(2500); // 2500 FCFA livraison normale
        }
    }

    private String genererNumeroCommande() {
        return "CMD-" + System.currentTimeMillis();
    }

    private String genererNumeroSuivi() {
        return "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String genererReferencePaiement() {
        return "PAY-" + System.currentTimeMillis();
    }

    private CommandeDTO convertToDTO(Commande commande) {
        CommandeDTO dto = modelMapper.map(commande, CommandeDTO.class);

        if (commande.getLignesCommande() != null) {
            dto.setLignesCommande(commande.getLignesCommande().stream()
                    .map(ligne -> {
                        CommandeDTO.LigneCommandeDTO ligneDTO = new CommandeDTO.LigneCommandeDTO();
                        ligneDTO.setIdLigneCommande(ligne.getIdLigneCommande());
                        ligneDTO.setProduitId(ligne.getProduit().getIdProduit());
                        ligneDTO.setQuantite(ligne.getQuantite());
                        ligneDTO.setPrixUnitaire(ligne.getPrixUnitaire());
                        ligneDTO.setSousTotal(ligne.getSousTotal());
                        ligneDTO.setNomProduitCommande(ligne.getNomProduitCommande());
                        ligneDTO.setRefProduitCommande(ligne.getRefProduitCommande());
                        return ligneDTO;
                    })
                    .collect(Collectors.toList()));
        }

        if (commande.getPaiement() != null) {
            Paiement paiement = commande.getPaiement();
            CommandeDTO.PaiementDTO paiementDTO = new CommandeDTO.PaiementDTO();
            paiementDTO.setIdPaiement(paiement.getIdPaiement());
            paiementDTO.setMethodePaiement(paiement.getMethodePaiement().name());
            paiementDTO.setStatutPaiement(paiement.getStatutPaiement().name());
            paiementDTO.setMontant(paiement.getMontant());
            paiementDTO.setDatePaiement(paiement.getDatePaiement());
            paiementDTO.setReferencePaiement(paiement.getReferencePaiement());
            paiementDTO.setReferenceExterne(paiement.getReferenceExterne());
            dto.setPaiement(paiementDTO);
        }

        return dto;
    }

    // Autres méthodes du service restent identiques...
    public Page<CommandeDTO> getAllCommandes(Pageable pageable) {
        return commandeRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    public Page<CommandeDTO> getCommandesByStatut(Commande.StatutCommande statutCommande, Pageable pageable) {
        return null;
    }





    /**
     * Récupérer toutes les commandes avec pagination et filtrage par statut
     */
    public Page<CommandeDTO> getAllCommandesAdmin(Pageable pageable, Commande.StatutCommande statut) {
        log.info("📋 Récupération commandes admin - Page: {}, Taille: {}, Statut: {}",
                pageable.getPageNumber(), pageable.getPageSize(), statut);

        Page<Commande> commandes;
        if (statut != null) {
            commandes = commandeRepository.findByStatutCommande(statut, pageable);
        } else {
            commandes = commandeRepository.findAllByOrderByDateCommandeDesc(pageable);
        }

        log.info("✅ {} commandes trouvées", commandes.getTotalElements());
        return commandes.map(this::convertToDTO);
    }

    /**
     * Mettre à jour le statut d'une commande
     */
    public CommandeDTO updateStatutCommande(Long commandeId, Commande.StatutCommande nouveauStatut) {
        log.info("🔄 Mise à jour statut commande - ID: {}, Nouveau statut: {}", commandeId, nouveauStatut);

        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec l'ID : " + commandeId));

        Commande.StatutCommande ancienStatut = commande.getStatutCommande();

        // Vérifier si le changement de statut est autorisé
        if (!isStatutChangeAllowed(ancienStatut, nouveauStatut)) {
            throw new IllegalStateException(
                    String.format("Changement de statut non autorisé : %s -> %s", ancienStatut, nouveauStatut)
            );
        }

        commande.setStatutCommande(nouveauStatut);
        commande.setDateModification(LocalDateTime.now());

        // Actions spéciales selon le statut
        switch (nouveauStatut) {
            case EXPEDIE:
                if (commande.getNumeroSuivi() == null) {
                    commande.setNumeroSuivi(genererNumeroSuivi());
                }
                break;
            case ANNULEE:
                // Remettre en stock les produits si la commande est annulée
                restaurerStock(commande);
                break;
        }

        commande = commandeRepository.save(commande);
        log.info("✅ Statut commande mis à jour - ID: {}, Statut: {}", commandeId, nouveauStatut);

        return convertToDTO(commande);
    }

    /**
     * Annuler une commande avec motif
     */
    public CommandeDTO annulerCommande(Long commandeId, String motif) {
        log.info("❌ Annulation commande - ID: {}, Motif: {}", commandeId, motif);

        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec l'ID : " + commandeId));

        // Vérifier que la commande peut être annulée
        if (commande.getStatutCommande() == Commande.StatutCommande.LIVREE) {
            throw new IllegalStateException("Une commande livrée ne peut pas être annulée");
        }

        if (commande.getStatutCommande() == Commande.StatutCommande.ANNULEE) {
            throw new IllegalStateException("Cette commande est déjà annulée");
        }

        commande.setStatutCommande(Commande.StatutCommande.ANNULEE);
       // commande.setMotifAnnulation(motif);
        commande.setDateModification(LocalDateTime.now());

        // Restaurer le stock des produits
        restaurerStock(commande);

        commande = commandeRepository.save(commande);
        log.info("✅ Commande annulée - ID: {}", commandeId);

        return convertToDTO(commande);
    }

    /**
     * Supprimer définitivement une commande (uniquement si annulée)
     */
    public void supprimerCommande(Long commandeId) {
        log.info("🗑️ Suppression commande - ID: {}", commandeId);

        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec l'ID : " + commandeId));

        // Vérifier que la commande peut être supprimée
        if (commande.getStatutCommande() != Commande.StatutCommande.ANNULEE) {
            throw new IllegalStateException("Seules les commandes annulées peuvent être supprimées définitivement");
        }

        commandeRepository.delete(commande);
        log.info("✅ Commande supprimée - ID: {}", commandeId);
    }

    // ============== MÉTHODES UTILITAIRES ==============

    /**
     * Vérifier si un changement de statut est autorisé
     */
    private boolean isStatutChangeAllowed(Commande.StatutCommande ancienStatut, Commande.StatutCommande nouveauStatut) {
        // Logique de validation des transitions de statut
        switch (ancienStatut) {
            case EN_ATTENTE:
                return nouveauStatut == Commande.StatutCommande.CONFIRMEE ||
                        nouveauStatut == Commande.StatutCommande.ANNULEE;

            case CONFIRMEE:
                return nouveauStatut == Commande.StatutCommande.PAYEE ||
                        nouveauStatut == Commande.StatutCommande.ANNULEE;

            case PAYEE:
                return nouveauStatut == Commande.StatutCommande.EN_PREPARATION ||
                        nouveauStatut == Commande.StatutCommande.ANNULEE;

            case EN_PREPARATION:
                return nouveauStatut == Commande.StatutCommande.EXPEDIE ||
                        nouveauStatut == Commande.StatutCommande.ANNULEE;

            case EXPEDIE:
                return nouveauStatut == Commande.StatutCommande.LIVREE;

            case LIVREE:
                return false; // Une commande livrée ne peut plus changer de statut

            case ANNULEE:
                return false; // Une commande annulée ne peut plus changer de statut

            default:
                return false;
        }
    }

    /**
     * Restaurer le stock des produits lors d'une annulation
     */
    private void restaurerStock(Commande commande) {
        if (commande.getLignesCommande() != null) {
            for (LigneCommande ligne : commande.getLignesCommande()) {
                try {
                    Produit produit = ligne.getProduit();
                    int nouvelleQuantite = produit.getStockDisponible() + ligne.getQuantite();
                    produitService.updateStock(produit.getIdProduit(), nouvelleQuantite);

                    log.info("🔄 Stock restauré - Produit: {} (+{})",
                            produit.getNomProduit(), ligne.getQuantite());
                } catch (Exception e) {
                    log.error("❌ Erreur lors de la restauration du stock pour le produit ID: {}",
                            ligne.getProduit().getIdProduit(), e);
                }
            }
        }
    }

    /**
     * Conversion DTO avec toutes les données nécessaires pour l'admin
     */
    private CommandeDTO convertToDTOWithFullDetails(Commande commande) {
        CommandeDTO dto = convertToDTO(commande);

        // Ajouter des informations spécifiques pour l'admin
        if (commande.getClient() != null) {
            dto.setClientId(commande.getClient().getIdClient());
        }

        // S'assurer que tous les champs sont bien mappés
       // dto.setMotifAnnulation(commande.getMotifAnnulation());
        dto.setDateModification(commande.getDateModification());

        return dto;
    }
}