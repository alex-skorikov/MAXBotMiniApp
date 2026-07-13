package org.maxbot.miniapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * CORS‑конфиг для Railway, который позволит фронту на Vercel обращаться к бэкенду на Railway
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {

        CorsConfiguration config = new CorsConfiguration();

        // Разрешаем фронт на Vercel
        config.addAllowedOrigin("https://max-webapp-sk.vercel.app");

        // Локальная разработка
        config.addAllowedOrigin("http://localhost:5173");

        // Разрешаем любые методы
        config.addAllowedMethod("*");

        // Разрешаем любые заголовки
        config.addAllowedHeader("*");

        // Если не используем куки — оставляем false
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
