package com.froidcheikh.ecommerce.service;


import com.froidcheikh.ecommerce.dto.ContactDTO;
import com.froidcheikh.ecommerce.entity.Contact;
import com.froidcheikh.ecommerce.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;

    @Transactional
    public Contact enregistrerContact(ContactDTO contactDTO) {
        Contact contact = new Contact();
        contact.setNom(contactDTO.getNom());
        contact.setPrenom(contactDTO.getPrenom());
        contact.setEmail(contactDTO.getEmail());
        contact.setTelephone(contactDTO.getTelephone());
        contact.setSujet(contactDTO.getSujet());
        contact.setMessage(contactDTO.getMessage());

        return contactRepository.save(contact);
    }

    public List<Contact> obtenirTousLesContacts() {
        return contactRepository.findAll();
    }

    public Contact obtenirContactParId(Long id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact non trouvé avec l'id: " + id));
    }

    public List<Contact> obtenirContactsParStatut(Contact.StatutContact statut) {
        return contactRepository.findByStatut(statut);
    }

    @Transactional
    public Contact mettreAJourStatut(Long id, Contact.StatutContact statut) {
        Contact contact = obtenirContactParId(id);
        contact.setStatut(statut);
        return contactRepository.save(contact);
    }

    @Transactional
    public void supprimerContact(Long id) {
        contactRepository.deleteById(id);
    }

    public long compterContactsNonLus() {
        return contactRepository.countByStatut(Contact.StatutContact.NON_LU);
    }
}