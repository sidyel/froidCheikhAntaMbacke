package com.froidcheikh.ecommerce.controller;


import com.froidcheikh.ecommerce.dto.ContactDTO;
import com.froidcheikh.ecommerce.entity.Contact;
import com.froidcheikh.ecommerce.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> creerContact(@Valid @RequestBody ContactDTO contactDTO) {
        try {
            Contact contact = contactService.enregistrerContact(contactDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Message envoyé avec succès");
            response.put("data", contact);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erreur lors de l'envoi du message: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping
    public ResponseEntity<List<Contact>> obtenirTousLesContacts() {
        List<Contact> contacts = contactService.obtenirTousLesContacts();
        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contact> obtenirContactParId(@PathVariable Long id) {
        Contact contact = contactService.obtenirContactParId(id);
        return ResponseEntity.ok(contact);
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<Contact>> obtenirContactsParStatut(@PathVariable Contact.StatutContact statut) {
        List<Contact> contacts = contactService.obtenirContactsParStatut(statut);
        return ResponseEntity.ok(contacts);
    }

    @PutMapping("/{id}/statut")
    public ResponseEntity<Contact> mettreAJourStatut(
            @PathVariable Long id,
            @RequestParam Contact.StatutContact statut) {
        Contact contact = contactService.mettreAJourStatut(id, statut);
        return ResponseEntity.ok(contact);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerContact(@PathVariable Long id) {
        contactService.supprimerContact(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/non-lus/count")
    public ResponseEntity<Map<String, Long>> compterContactsNonLus() {
        long count = contactService.compterContactsNonLus();
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }


    // Dans ContactController.java
    @GetMapping("/recents")
    public ResponseEntity<List<Contact>> obtenirContactsRecents(
            @RequestParam(defaultValue = "5") int limit) {
        List<Contact> contacts = contactService.obtenirTousLesContacts()
                .stream()
                .sorted((c1, c2) -> c2.getDateEnvoi().compareTo(c1.getDateEnvoi()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(contacts);
    }
}