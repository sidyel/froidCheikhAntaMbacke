package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.controller.AdminController;
import com.froidcheikh.ecommerce.dto.AuthDTO;
import com.froidcheikh.ecommerce.dto.ClientDTO;
import com.froidcheikh.ecommerce.entity.Client;
import com.froidcheikh.ecommerce.entity.Adresse;
import com.froidcheikh.ecommerce.entity.Commande;
import com.froidcheikh.ecommerce.exception.ResourceNotFoundException;
import com.froidcheikh.ecommerce.repository.ClientRepository;
import com.froidcheikh.ecommerce.repository.AdresseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final AdresseRepository adresseRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    public ClientDTO getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID : " + id));
        return convertToDTO(client);
    }

    public ClientDTO getClientByEmail(String email) {
        Client client = clientRepository.findByEmailWithAdresses(email)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'email : " + email));
        return convertToDTO(client);
    }

    public ClientDTO createClient(ClientDTO clientDTO) {
        // Vérifier si l'email existe déjà
        if (clientRepository.existsByEmail(clientDTO.getEmail())) {
            throw new RuntimeException("Un client avec cet email existe déjà");
        }

        Client client = convertToEntity(clientDTO);
        client = clientRepository.save(client);

        log.info("Client créé avec l'ID : {}", client.getIdClient());
        return convertToDTO(client);
    }

    public ClientDTO updateClient(Long id, ClientDTO clientDTO) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID : " + id));

        // Vérifier si l'email est déjà utilisé par un autre client
        if (!client.getEmail().equals(clientDTO.getEmail()) &&
                clientRepository.existsByEmail(clientDTO.getEmail())) {
            throw new RuntimeException("Un client avec cet email existe déjà");
        }

        client.setNom(clientDTO.getNom());
        client.setPrenom(clientDTO.getPrenom());
        client.setEmail(clientDTO.getEmail());
        client.setTelephone(clientDTO.getTelephone());
        client.setDateNaissance(clientDTO.getDateNaissance());
        client.setGenre(clientDTO.getGenre());

        client = clientRepository.save(client);
        log.info("Client mis à jour avec l'ID : {}", client.getIdClient());

        return convertToDTO(client);
    }

    public void updatePassword(Long clientId, String nouveauMotDePasse) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID : " + clientId));

        client.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        clientRepository.save(client);

        log.info("Mot de passe mis à jour pour le client : {}", clientId);
    }

    public void activerClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID : " + clientId));

        client.setActif(true);
        clientRepository.save(client);

        log.info("Client activé : {}", clientId);
    }

    public void desactiverClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID : " + clientId));

        client.setActif(false);
        clientRepository.save(client);

        log.info("Client désactivé : {}", clientId);
    }

    // Gestion des adresses
    public ClientDTO.AdresseDTO ajouterAdresse(Long clientId, ClientDTO.AdresseDTO adresseDTO) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID : " + clientId));

        Adresse adresse = convertToAdresseEntity(adresseDTO);
        adresse.setClient(client);

        // Si c'est la première adresse ou si elle est marquée comme principale
        if (client.getAdresses().isEmpty() || adresseDTO.getAdressePrincipale()) {
            // Démarquer les autres adresses comme non principales
            if (adresseDTO.getAdressePrincipale()) {
                client.getAdresses().forEach(a -> a.setAdressePrincipale(false));
            }
            adresse.setAdressePrincipale(true);
        }

        adresse = adresseRepository.save(adresse);
        log.info("Adresse ajoutée pour le client : {}", clientId);

        return convertToAdresseDTO(adresse);
    }

    public ClientDTO.AdresseDTO updateAdresse(Long clientId, Long adresseId, ClientDTO.AdresseDTO adresseDTO) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID : " + clientId));

        Adresse adresse = adresseRepository.findById(adresseId)
                .orElseThrow(() -> new ResourceNotFoundException("Adresse non trouvée avec l'ID : " + adresseId));

        if (!adresse.getClient().getIdClient().equals(clientId)) {
            throw new RuntimeException("Cette adresse n'appartient pas à ce client");
        }

        adresse.setLigne1(adresseDTO.getLigne1());
        adresse.setLigne2(adresseDTO.getLigne2());
        adresse.setVille(adresseDTO.getVille());
        adresse.setCodePostal(adresseDTO.getCodePostal());
        adresse.setPays(adresseDTO.getPays());
        adresse.setTelephone(adresseDTO.getTelephone());

        if (adresseDTO.getAdressePrincipale() != null) {
            adresse.setAdressePrincipale(adresseDTO.getAdressePrincipale());

            if (adresseDTO.getAdressePrincipale()) {
                // Démarquer les autres adresses comme non principales
                client.getAdresses().stream()
                        .filter(a -> !a.getIdAdresse().equals(adresseId))
                        .forEach(a -> a.setAdressePrincipale(false));
            }
        }

        adresse = adresseRepository.save(adresse);
        log.info("Adresse mise à jour : {}", adresseId);

        return convertToAdresseDTO(adresse);
    }

    public void supprimerAdresse(Long clientId, Long adresseId) {
        Adresse adresse = adresseRepository.findById(adresseId)
                .orElseThrow(() -> new ResourceNotFoundException("Adresse non trouvée avec l'ID : " + adresseId));

        if (!adresse.getClient().getIdClient().equals(clientId)) {
            throw new RuntimeException("Cette adresse n'appartient pas à ce client");
        }

        adresseRepository.deleteById(adresseId);
        log.info("Adresse supprimée : {}", adresseId);
    }

    public List<ClientDTO.AdresseDTO> getAdressesClient(Long clientId) {
        return adresseRepository.findByClientIdClient(clientId)
                .stream()
                .map(this::convertToAdresseDTO)
                .collect(Collectors.toList());
    }

    // Gestion de la wishlist
    public void ajouterAWishlist(Long clientId, Long produitId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID : " + clientId));

        if (!client.getWishlist().contains(produitId)) {
            client.getWishlist().add(produitId);
            clientRepository.save(client);
            log.info("Produit {} ajouté à la wishlist du client {}", produitId, clientId);
        }
    }

    public void retirerDeWishlist(Long clientId, Long produitId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID : " + clientId));

        client.getWishlist().remove(produitId);
        clientRepository.save(client);
        log.info("Produit {} retiré de la wishlist du client {}", produitId, clientId);
    }

    public List<Long> getWishlist(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID : " + clientId));

        return client.getWishlist();
    }

    private ClientDTO convertToDTO(Client client) {
        ClientDTO dto = modelMapper.map(client, ClientDTO.class);

        if (client.getAdresses() != null) {
            dto.setAdresses(client.getAdresses().stream()
                    .map(this::convertToAdresseDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }



    // Dans ClientService

    // Méthode pour l'inscription (réception d'un AuthDTO.RegisterRequest)
    public ClientDTO registerClient(AuthDTO.RegisterRequest registerRequest) {
        if (clientRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Un client avec cet email existe déjà");
        }

        Client client = convertToEntity(registerRequest);
        client = clientRepository.save(client);

        log.info("Client enregistré (inscription) avec l'ID : {}", client.getIdClient());
        return convertToDTO(client);
    }

    // Méthode existante pour créer un client depuis ClientDTO (ex : admin)
    public ClientDTO createClient1(ClientDTO clientDTO) {
        if (clientRepository.existsByEmail(clientDTO.getEmail())) {
            throw new RuntimeException("Un client avec cet email existe déjà");
        }
        Client client = convertToEntity(clientDTO);
        client = clientRepository.save(client);
        log.info("Client créé avec l'ID : {}", client.getIdClient());
        return convertToDTO(client);
    }

    // Conversion ClientDTO -> Client (sans encodage de mot de passe)
    private Client convertToEntity(ClientDTO dto) {
        return modelMapper.map(dto, Client.class);
    }

    // Conversion RegisterRequest -> Client (ici on encode le mot de passe)
    private Client convertToEntity(AuthDTO.RegisterRequest registerRequest) {
        Client client = modelMapper.map(registerRequest, Client.class);
        client.setMotDePasse(passwordEncoder.encode(registerRequest.getMotDePasse()));
        // définir d'éventuelles valeurs par défaut (actif, rôle, etc.)
        return client;
    }


    private ClientDTO.AdresseDTO convertToAdresseDTO(Adresse adresse) {
        return modelMapper.map(adresse, ClientDTO.AdresseDTO.class);
    }

    private Adresse convertToAdresseEntity(ClientDTO.AdresseDTO dto) {
        return modelMapper.map(dto, Adresse.class);
    }

    public Page<ClientDTO> getAllClients(Pageable pageable) {
        return clientRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    public List<ClientDTO> searchClients(String searchTerm) {
        // Implémentation de la recherche de clients
        return clientRepository.findAll()
                .stream()
                .filter(client ->
                        client.getNom().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                client.getPrenom().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                client.getEmail().toLowerCase().contains(searchTerm.toLowerCase())
                )
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ==================== MÉTHODES POUR L'ADMINISTRATION ====================



    public Page<ClientDTO> searchClients(String searchTerm, Pageable pageable) {
        Page<Client> clients = clientRepository.searchClients(searchTerm, pageable);
        return clients.map(this::convertToDTO);
    }

    public ClientDTO createClientByAdmin(AdminController.CreateClientRequest request) {
        // Vérifier si l'email existe déjà
        if (clientRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un client avec cet email existe déjà");
        }

        Client client = new Client();
        client.setNom(request.getNom());
        client.setPrenom(request.getPrenom());
        client.setEmail(request.getEmail());
        client.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        client.setTelephone(request.getTelephone());
        client.setDateNaissance(request.getDateNaissance());
        client.setGenre(request.getGenre());
        client.setActif(request.getActif());

        client = clientRepository.save(client);
        log.info("Client créé par admin avec l'ID : {}", client.getIdClient());

        return convertToDTO(client);
    }

    public ClientDTO updateClientByAdmin(Long clientId, AdminController.UpdateClientRequest request) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID : " + clientId));

        // Vérifier si l'email est déjà utilisé par un autre client
        if (!client.getEmail().equals(request.getEmail()) &&
                clientRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un client avec cet email existe déjà");
        }

        client.setNom(request.getNom());
        client.setPrenom(request.getPrenom());
        client.setEmail(request.getEmail());
        client.setTelephone(request.getTelephone());
        client.setDateNaissance(request.getDateNaissance());
        client.setGenre(request.getGenre());
        client.setActif(request.getActif());

        client = clientRepository.save(client);
        log.info("Client mis à jour par admin avec l'ID : {}", client.getIdClient());

        return convertToDTO(client);
    }

    public void deleteClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID : " + clientId));

        // Vérifier s'il a des commandes en cours
        boolean hasActiveOrders = client.getCommandes() != null &&
                client.getCommandes().stream()
                        .anyMatch(commande ->
                                commande.getStatutCommande() == Commande.StatutCommande.EN_ATTENTE ||
                                        commande.getStatutCommande() == Commande.StatutCommande.CONFIRMEE ||
                                        commande.getStatutCommande() == Commande.StatutCommande.PAYEE ||
                                        commande.getStatutCommande() == Commande.StatutCommande.EN_PREPARATION ||
                                        commande.getStatutCommande() == Commande.StatutCommande.EXPEDIE);

        if (hasActiveOrders) {
            throw new RuntimeException("Impossible de supprimer un client avec des commandes en cours");
        }

        clientRepository.deleteById(clientId);
        log.info("Client supprimé avec l'ID : {}", clientId);
    }

    public Map<String, Object> getClientStatistics(Long clientId) {
        Client client = clientRepository.findByIdWithCommandes(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID : " + clientId));

        Map<String, Object> stats = new HashMap<>();

        // Statistiques des commandes
        List<Commande> commandes = client.getCommandes();
        if (commandes != null) {
            stats.put("totalCommandes", commandes.size());
            stats.put("commandesLivrees", commandes.stream()
                    .filter(c -> c.getStatutCommande() == Commande.StatutCommande.LIVREE)
                    .count());
            stats.put("commandesEnCours", commandes.stream()
                    .filter(c -> c.getStatutCommande() != Commande.StatutCommande.LIVREE &&
                            c.getStatutCommande() != Commande.StatutCommande.ANNULEE)
                    .count());

            // Montant total des achats
            BigDecimal montantTotal = commandes.stream()
                    .filter(c -> c.getStatutCommande() == Commande.StatutCommande.LIVREE)
                    .map(Commande::getMontantTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.put("montantTotalAchats", montantTotal);

            // Panier moyen
            long commandesLivrees = (Long) stats.get("commandesLivrees");
            if (commandesLivrees > 0) {
                stats.put("panierMoyen", montantTotal.divide(BigDecimal.valueOf(commandesLivrees), 2, RoundingMode.HALF_UP));
            } else {
                stats.put("panierMoyen", BigDecimal.ZERO);
            }

            // Dernière commande
            Optional<Commande> derniereCommande = commandes.stream()
                    .max((c1, c2) -> c1.getDateCommande().compareTo(c2.getDateCommande()));
            if (derniereCommande.isPresent()) {
                stats.put("derniereCommande", derniereCommande.get().getDateCommande());
            }
        } else {
            stats.put("totalCommandes", 0);
            stats.put("commandesLivrees", 0);
            stats.put("commandesEnCours", 0);
            stats.put("montantTotalAchats", BigDecimal.ZERO);
            stats.put("panierMoyen", BigDecimal.ZERO);
        }

        // Wishlist
        stats.put("produitsWishlist", client.getWishlist() != null ? client.getWishlist().size() : 0);

        // Adresses
        stats.put("nombreAdresses", client.getAdresses() != null ? client.getAdresses().size() : 0);

        return stats;
    }

    // Actions en lot
    public Map<String, Object> batchActivateClients(List<Long> clientIds) {
        Map<String, Object> result = new HashMap<>();
        int success = 0;
        int errors = 0;

        for (Long clientId : clientIds) {
            try {
                activerClient(clientId);
                success++;
            } catch (Exception e) {
                errors++;
                log.error("Erreur lors de l'activation du client {}: {}", clientId, e.getMessage());
            }
        }

        result.put("success", success);
        result.put("errors", errors);
        result.put("total", clientIds.size());

        log.info("Activation en lot terminée: {} succès, {} erreurs sur {} clients", success, errors, clientIds.size());

        return result;
    }

    public Map<String, Object> batchDeactivateClients(List<Long> clientIds) {
        Map<String, Object> result = new HashMap<>();
        int success = 0;
        int errors = 0;

        for (Long clientId : clientIds) {
            try {
                desactiverClient(clientId);
                success++;
            } catch (Exception e) {
                errors++;
                log.error("Erreur lors de la désactivation du client {}: {}", clientId, e.getMessage());
            }
        }

        result.put("success", success);
        result.put("errors", errors);
        result.put("total", clientIds.size());

        log.info("Désactivation en lot terminée: {} succès, {} erreurs sur {} clients", success, errors, clientIds.size());

        return result;
    }

    public Map<String, Object> batchDeleteClients(List<Long> clientIds) {
        Map<String, Object> result = new HashMap<>();
        int success = 0;
        int errors = 0;

        for (Long clientId : clientIds) {
            try {
                deleteClient(clientId);
                success++;
            } catch (Exception e) {
                errors++;
                log.error("Erreur lors de la suppression du client {}: {}", clientId, e.getMessage());
            }
        }

        result.put("success", success);
        result.put("errors", errors);
        result.put("total", clientIds.size());

        log.info("Suppression en lot terminée: {} succès, {} erreurs sur {} clients", success, errors, clientIds.size());

        return result;
    }
}