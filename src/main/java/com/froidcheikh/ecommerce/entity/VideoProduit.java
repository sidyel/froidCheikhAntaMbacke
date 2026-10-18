package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_produit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoProduit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_video", nullable = false, length = 20)
    private TypeVideo type;

    /** ID extrait de l'URL : videoId YouTube/Facebook, ou publicId Cloudinary. */
    @Column(name = "video_id", length = 255)
    private String videoId;

    /** URL originale pour YouTube/Facebook, ou secure_url Cloudinary. */
    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    /** URL de la miniature (img.youtube.com, screenshot Cloudinary, ou image personnalisée pour Facebook). */
    @Column(name = "url_apercu", columnDefinition = "TEXT")
    private String urlApercu;

    @Column(name = "titre", length = 255)
    private String titre;

    @Column(name = "ordre", nullable = false)
    private int ordre = 0;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;
}
