package com.froidcheikh.ecommerce.controller;

import com.froidcheikh.ecommerce.service.CatalogueFeedService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CatalogueFeedController {

    private final CatalogueFeedService feedService;

    public CatalogueFeedController(CatalogueFeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping(value = "/feeds/meta.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> metaFeed() {
        return ResponseEntity.ok(feedService.generateMetaFeed());
    }

    @GetMapping(value = "/feeds/google.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> googleFeed() {
        return ResponseEntity.ok(feedService.generateGoogleFeed());
    }

    @GetMapping("/api/admin/catalogue/exclusions")
    public ResponseEntity<List<CatalogueFeedService.ExclusionDTO>> exclusions() {
        return ResponseEntity.ok(feedService.getExclusions());
    }

    @GetMapping("/api/admin/catalogue/rapport")
    public ResponseEntity<CatalogueFeedService.RapportFeedDTO> rapport() {
        return ResponseEntity.ok(feedService.getRapport());
    }
}
