package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class FileController {

    private final FileService fileService;

    /**
     * Vérifie si une URL Cloudinary est valide
     * Les fichiers sont maintenant servis directement par Cloudinary
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> fileExists(@RequestParam String filePath) {
        // Avec Cloudinary, on vérifie juste si l'URL n'est pas vide
        boolean exists = filePath != null && !filePath.isEmpty();
        log.info("File exists check: {} -> {}", filePath, exists);
        return ResponseEntity.ok(exists);
    }
}