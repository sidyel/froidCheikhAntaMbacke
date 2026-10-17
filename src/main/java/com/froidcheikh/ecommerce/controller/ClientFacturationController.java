package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.dto.ClientFacturationDTO;
import com.froidcheikh.ecommerce.entity.ClientFacturation;
import com.froidcheikh.ecommerce.repository.ClientFacturationRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/clients-facturation")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','GESTIONNAIRE')")
@RequiredArgsConstructor
public class ClientFacturationController {

    private final ClientFacturationRepository repo;

    @GetMapping
    public List<ClientFacturationDTO.Response> rechercher(
            @RequestParam(defaultValue = "") String q) {
        List<ClientFacturation> clients = q.isBlank()
                ? repo.findAll()
                : repo.rechercher(q);
        return clients.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ClientFacturationDTO.Response getById(@PathVariable Long id) {
        return repo.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable : " + id));
    }

    @PostMapping
    public ResponseEntity<ClientFacturationDTO.Response> creer(
            @Valid @RequestBody ClientFacturationDTO.CreateRequest req) {
        ClientFacturation c = new ClientFacturation();
        c.setNom(req.getNom());
        c.setPrenom(req.getPrenom());
        c.setTelephone(req.getTelephone());
        c.setEmail(req.getEmail());
        c.setAdresse(req.getAdresse());
        c.setNinea(req.getNinea());
        c.setRccm(req.getRccm());
        return ResponseEntity.ok(toResponse(repo.save(c)));
    }

    @PutMapping("/{id}")
    public ClientFacturationDTO.Response modifier(
            @PathVariable Long id,
            @Valid @RequestBody ClientFacturationDTO.CreateRequest req) {
        ClientFacturation c = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable : " + id));
        c.setNom(req.getNom());
        c.setPrenom(req.getPrenom());
        c.setTelephone(req.getTelephone());
        c.setEmail(req.getEmail());
        c.setAdresse(req.getAdresse());
        c.setNinea(req.getNinea());
        c.setRccm(req.getRccm());
        return toResponse(repo.save(c));
    }

    private ClientFacturationDTO.Response toResponse(ClientFacturation c) {
        ClientFacturationDTO.Response r = new ClientFacturationDTO.Response();
        r.setId(c.getId());
        r.setNom(c.getNom());
        r.setPrenom(c.getPrenom());
        r.setTelephone(c.getTelephone());
        r.setEmail(c.getEmail());
        r.setAdresse(c.getAdresse());
        r.setNinea(c.getNinea());
        r.setRccm(c.getRccm());
        r.setDateCreation(c.getDateCreation());
        return r;
    }
}
