package org.maxbot.miniapp.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/subscriptions")
public class MaxSubscriptionsController {

    private final WebClient webClient;

    public MaxSubscriptionsController(@Value("${max.api.token}") String token) {
        this.webClient = WebClient.builder()
                .baseUrl("https://platform-api2.max.ru")
                .defaultHeader("Authorization", token)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Создаёт подписку на webhook
     */
    @PostMapping
    public Mono<String> createSubscription() {

        Map<String, Object> body = Map.of(
                "url", "https://maxbotminiapp-production.up.railway.app/webhook"
        );

        System.out.println(">>> Creating webhook subscription: " + body);

        return webClient.post()
                .uri("/subscriptions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(resp -> System.out.println(">>> Subscription created: " + resp));
    }

    /**
     * Получает список всех подписок
     */
    @GetMapping
    public Mono<String> getSubscriptions() {

        System.out.println(">>> Getting webhook subscriptions");

        return webClient.get()
                .uri("/subscriptions")
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(resp -> System.out.println(">>> Subscriptions list: " + resp));
    }
}

