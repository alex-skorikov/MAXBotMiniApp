package org.maxbot.miniapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class CorsWebFluxConfig {

//    @Bean
//    public CorsWebFilter corsWebFilter() {
//        CorsConfiguration config = new CorsConfiguration();
//        config.addAllowedOrigin("https://max-webapp-five.vercel.app");
//        config.addAllowedOriginPattern("*");
//        config.addAllowedMethod("*");
//        config.addAllowedHeader("*");
//        config.setAllowCredentials(true);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//
//        return new CorsWebFilter(source);
//    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // 1. Указываем точный URL вашего фронтенда на Vercel
        // ВАЖНО: Не используйте "*", если фронт передает куки, токены авторизации или initData
        corsConfig.setAllowedOrigins(Arrays.asList(
                "https://max-webapp-five.vercel.app", // Ваш продакшн домен Vercel
                "http://localhost:3000"              // Для локальной отладки мини-приложения
        ));

        // 2. Разрешаем необходимые HTTP-методы
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 3. Разрешаем любые заголовки (или перечислите конкретные: Authorization, Content-Type и т.д.)
        corsConfig.setAllowedHeaders(Collections.singletonList("*"));

        // 4. Обязательно разрешаем отправку учетных данных (куки, Authorization заголовок)
        corsConfig.setAllowCredentials(true);

        // 5. Время кэширования предварительного (preflight) OPTIONS-запроса браузером (1 час)
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Применяем настройки CORS ко всем эндпоинтам бэкенда
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

}


