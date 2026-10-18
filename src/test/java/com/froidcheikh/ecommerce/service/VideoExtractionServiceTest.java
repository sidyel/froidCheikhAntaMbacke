package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.entity.TypeVideo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class VideoExtractionServiceTest {

    private VideoExtractionService service;

    @BeforeEach
    void setUp() {
        service = new VideoExtractionService();
    }

    // ── YouTube ───────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            "https://youtube.com/watch?v=dQw4w9WgXcQ&feature=share",
            "https://www.youtube.com/watch?list=PL123&v=dQw4w9WgXcQ"
    })
    void devraitExtraireIdYoutube(String url) {
        VideoExtractionService.ExtractionResult result = service.extraire(url);

        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo(TypeVideo.YOUTUBE);
        assertThat(result.videoId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(result.urlApercu())
                .isEqualTo("https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg");
    }

    @Test
    void devraitGenererUrlEmbedYoutube() {
        String embed = service.buildYoutubeEmbedUrl("dQw4w9WgXcQ");
        assertThat(embed).startsWith("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ");
        assertThat(embed).contains("autoplay=1");
    }

    // ── Facebook ──────────────────────────────────────────────────────────────

    @Test
    void devraitExtraireIdFacebookLong() {
        String url = "https://www.facebook.com/watch/?v=1234567890";
        VideoExtractionService.ExtractionResult result = service.extraire(url);

        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo(TypeVideo.FACEBOOK);
        assertThat(result.videoId()).isEqualTo("1234567890");
    }

    @Test
    void devraitExtraireIdFacebookShort() {
        String url = "https://fb.watch/abcXYZ123";
        VideoExtractionService.ExtractionResult result = service.extraire(url);

        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo(TypeVideo.FACEBOOK);
        assertThat(result.videoId()).isEqualTo("abcXYZ123");
    }

    @Test
    void devraitGenererUrlEmbedFacebook() {
        String url = "https://www.facebook.com/watch/?v=1234567890";
        String embed = service.buildFacebookEmbedUrl(url);

        assertThat(embed).isNotNull();
        assertThat(embed).contains("facebook.com/plugins/video.php");
        assertThat(embed).contains("href=");
    }

    // ── URL invalide ──────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.google.com",
            "https://tiktok.com/@user/video/123",
            "pas-une-url",
            "https://vimeo.com/123456789",
            ""
    })
    void devraitRetournerNullPourUrlNonReconnue(String url) {
        VideoExtractionService.ExtractionResult result = service.extraire(url);
        assertThat(result).isNull();
    }

    // ── Miniature Cloudinary ──────────────────────────────────────────────────

    @Test
    void devraitGenererApercuCloudinary() {
        String url = "https://res.cloudinary.com/demo/video/upload/v123/produits/videos/1/ma-video.mp4";
        String apercu = service.buildCloudinaryApercu(url);

        assertThat(apercu).isNotNull();
        assertThat(apercu).contains("/upload/so_auto,w_320,h_180,c_fill,f_jpg,q_auto/");
        assertThat(apercu).endsWith(".jpg");
    }

    @Test
    void devraitRetournerNullPourUrlCloudinaryInvalide() {
        assertThat(service.buildCloudinaryApercu(null)).isNull();
        assertThat(service.buildCloudinaryApercu("https://example.com/video.mp4")).isNull();
    }
}
