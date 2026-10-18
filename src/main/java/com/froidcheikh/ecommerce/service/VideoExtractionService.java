package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.entity.TypeVideo;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrait l'identifiant vidéo et la miniature à partir d'une URL YouTube ou Facebook.
 * Formats YouTube supportés : watch?v=, youtu.be/, /shorts/, /embed/.
 * Formats Facebook supportés : facebook.com/watch/?v=, fb.watch/.
 */
@Service
public class VideoExtractionService {

    private static final Pattern YT_PATTERN = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|shorts/|embed/)|youtu\\.be/)([A-Za-z0-9_-]{11})"
    );

    private static final Pattern FB_WATCH_PATTERN = Pattern.compile(
            "facebook\\.com/(?:[^/]+/)?(?:videos/|watch/?)\\?(?:.*&)?v=(\\d+)"
    );

    private static final Pattern FB_SHORT_PATTERN = Pattern.compile(
            "fb\\.watch/([A-Za-z0-9_-]+)"
    );

    public record ExtractionResult(TypeVideo type, String videoId, String urlApercu) {}

    /**
     * Tente d'extraire les infos d'une URL YouTube ou Facebook.
     * Retourne null si l'URL n'est pas reconnue.
     */
    public ExtractionResult extraire(String url) {
        if (url == null) return null;
        String normalized = url.trim();

        // YouTube
        Matcher yt = YT_PATTERN.matcher(normalized);
        if (yt.find()) {
            String id = yt.group(1);
            String apercu = "https://img.youtube.com/vi/" + id + "/hqdefault.jpg";
            return new ExtractionResult(TypeVideo.YOUTUBE, id, apercu);
        }

        // Facebook (lien long avec v=)
        Matcher fb = FB_WATCH_PATTERN.matcher(normalized);
        if (fb.find()) {
            String id = fb.group(1);
            return new ExtractionResult(TypeVideo.FACEBOOK, id, null);
        }

        // Facebook (lien court fb.watch/)
        Matcher fbs = FB_SHORT_PATTERN.matcher(normalized);
        if (fbs.find()) {
            String id = fbs.group(1);
            return new ExtractionResult(TypeVideo.FACEBOOK, id, null);
        }

        return null;
    }

    /** Construit l'URL d'embed YouTube (youtube-nocookie.com). */
    public String buildYoutubeEmbedUrl(String videoId) {
        return "https://www.youtube-nocookie.com/embed/" + videoId + "?autoplay=1&rel=0";
    }

    /** Construit l'URL d'embed Facebook (iframe officielle légère). */
    public String buildFacebookEmbedUrl(String originalUrl) {
        try {
            return "https://www.facebook.com/plugins/video.php?href="
                    + java.net.URLEncoder.encode(originalUrl, java.nio.charset.StandardCharsets.UTF_8)
                    + "&show_text=false&appId=";
        } catch (Exception e) {
            return null;
        }
    }

    /** Construit la miniature Cloudinary d'une vidéo (screenshot auto). */
    public String buildCloudinaryApercu(String secureUrl) {
        if (secureUrl == null || !secureUrl.contains("/upload/")) return null;
        return secureUrl
                .replace("/upload/", "/upload/so_auto,w_320,h_180,c_fill,f_jpg,q_auto/")
                .replaceAll("\\.[^.]+$", ".jpg");
    }
}
