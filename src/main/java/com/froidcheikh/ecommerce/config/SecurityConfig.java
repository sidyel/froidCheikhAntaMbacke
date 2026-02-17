package com.froidcheikh.ecommerce.config;

import com.froidcheikh.ecommerce.security.JwtAuthenticationFilter;
import com.froidcheikh.ecommerce.security.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Endpoints publics d'authentification
                        .requestMatchers("/api/auth/**").permitAll()

                        // Endpoints publics pour les produits
                        .requestMatchers(HttpMethod.GET, "/api/produits/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/marques/**").permitAll()

                        // Endpoints publics pour commandes invités
                        .requestMatchers(HttpMethod.POST, "/api/commandes/invite").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/commandes/invite/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/commandes/numero/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/commandes/{id:[0-9]+}").permitAll()

                        // Endpoints publics de paiement
                        .requestMatchers(HttpMethod.POST, "/api/commandes/*/paiement").permitAll()

                        // Endpoints pour les fichiers uploads
                        .requestMatchers("/uploads/**").permitAll()

                        // Endpoints de test (optionnel - à retirer en production)
                        .requestMatchers("/api/test/**").permitAll()

                        // ✅ ENDPOINTS CLIENTS AUTHENTIFIÉS
                        .requestMatchers("/api/clients/profil/**").hasRole("CLIENT")
                        .requestMatchers("/api/clients/commandes/**").hasRole("CLIENT")
                        .requestMatchers("/api/clients/adresses/**").hasRole("CLIENT")
                        .requestMatchers("/api/clients/wishlist/**").hasRole("CLIENT") // ✅ CRUCIAL
                        .requestMatchers("/api/clients/wishlist").hasRole("CLIENT")    // ✅ CRUCIAL
                        .requestMatchers("/api/clients/**").hasRole("CLIENT")

                        // Endpoints pour les commandes de clients connectés
                        .requestMatchers("/api/commandes/client/**").hasRole("CLIENT")

                        // Endpoints admin
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "GESTIONNAIRE")

                        // Tous les autres endpoints nécessitent une authentification
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Non authentifié\", \"message\": \"" + authException.getMessage() + "\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Accès refusé\", \"message\": \"" + accessDeniedException.getMessage() + "\"}");
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Origines autorisées
        configuration.setAllowedOriginPatterns(List.of("*"));

        // Méthodes HTTP autorisées
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Headers autorisés
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Autoriser les credentials
        configuration.setAllowCredentials(true);

        // Cache des preflight requests
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}