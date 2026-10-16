package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.entity.ParametresSite;
import com.froidcheikh.ecommerce.repository.ParametresSiteRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RobotsController {

    private final ParametresSiteRepository parametresSiteRepository;

    public RobotsController(ParametresSiteRepository parametresSiteRepository) {
        this.parametresSiteRepository = parametresSiteRepository;
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> robots() {
        String baseUrl = parametresSiteRepository.findById(1L)
                .map(ParametresSite::getSiteUrl)
                .filter(u -> u != null && !u.isBlank())
                .orElse("http://localhost:4000");

        String content = "User-agent: *\n" +
                "Disallow: /admin\n" +
                "Disallow: /panier\n" +
                "Disallow: /commande\n" +
                "Disallow: /checkout\n" +
                "Disallow: /compte\n" +
                "Disallow: /mes-commandes\n" +
                "Disallow: /profil\n" +
                "Disallow: /telecharger\n" +
                "Disallow: /suivi-commande\n" +
                "\n" +
                "Sitemap: " + baseUrl + "/sitemap.xml\n";

        return ResponseEntity.ok(content);
    }
}
