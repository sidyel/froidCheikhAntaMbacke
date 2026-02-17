package com.froidcheikh.ecommerce.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.upload")
public class FileUploadProperties {
    private String dir = "uploads";
    private long maxFileSize = 5242880; // 5MB
    private String[] allowedImageTypes = {
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    };
    private String[] allowedPdfTypes = {"application/pdf"};
}