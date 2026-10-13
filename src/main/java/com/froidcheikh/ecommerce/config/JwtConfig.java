package com.froidcheikh.ecommerce.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {
    private String secret;
    private long expiration = 86400000; // 24 heures en millisecondes
    private long refreshExpiration = 604800000; // 7 jours en millisecondes
}