package com.epiis.projectcasaketteler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(false);

        // Cubrimos todas las formas en las que tu Xiaomi se puede presentar ante Spring
        // Boot
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost",
                "https://localhost",
                "capacitor://localhost",
                "http://192.168.43.46", // <-- La IP de tu celular en HTTP
                "https://192.168.43.46", // <-- La IP de tu celular en HTTPS
                "http://192.168.43.46:8001" // Por si acaso intentes levantar un puerto
        ));

        // Esto le permite usar comodines si se conecta desde otra subred en el futuro
        config.addAllowedOriginPattern("http://192.168.43.*");
        config.addAllowedOriginPattern("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}