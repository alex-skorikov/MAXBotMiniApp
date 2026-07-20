package org.maxbot.miniapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig {
    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
//                .defaultHeader("User-Agent", "curl/8.0.1")
                .defaultHeader("Accept", "*/*")
//                .defaultHeader("Accept-Encoding", "gzip, deflate, br")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, "*/*")
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.info("HTTP REQUEST → {} {}", request.method(), request.url());
            request.headers().forEach((name, values) ->
                    values.forEach(value -> log.debug("Header: {}={}", name, value))
            );
            return Mono.just(request);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.info("HTTP RESPONSE ← Status: {}", response.statusCode());
            return Mono.just(response);
        });
    }
}

