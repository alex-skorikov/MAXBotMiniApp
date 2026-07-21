package org.maxbot.miniapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebFilter;

@Configuration
public class CorsWebFluxConfig {

//    @Bean
//    public CorsWebFilter corsWebFilter() {
//        CorsConfiguration config = new CorsConfiguration();
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
        CorsConfiguration config = new CorsConfiguration();

        // Доверяем Vercel
        config.addAllowedOrigin("https://max-webapp-five.vercel.app");

        // Доверяем Telegram Web
        config.addAllowedOrigin("https://web.telegram.org");
        config.addAllowedOrigin("https://telegram.org");

        // Доверяем MiniApp WebView (Origin: null)
        config.addAllowedOriginPattern("*");

        // Явно разрешаем методы
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("OPTIONS");

        // Явно разрешаем заголовки
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("X-Request-Id");
        config.addAllowedHeader("X-Requested-With");
        config.addAllowedHeader("*");

        // Не нужны куки
        config.setAllowCredentials(false);

        // Важно для WebView
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }

    @Bean
    public WebFilter logOriginFilter() {
        return (exchange, chain) -> {
            System.out.println("🌐 Origin: " + exchange.getRequest().getHeaders().getOrigin());
            return chain.filter(exchange);
        };
    }

}


