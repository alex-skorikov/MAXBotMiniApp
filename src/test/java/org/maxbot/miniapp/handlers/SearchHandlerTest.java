package org.maxbot.miniapp.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.maxbot.miniapp.dto.patent.PatentHit;
import org.maxbot.miniapp.dto.patent.PatentSearchRequest;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.maxbot.miniapp.repository.ContextRepository;
import org.maxbot.miniapp.service.PatentService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchHandlerTest {

    @Mock
    private PatentService patentService;

    @Mock
    private MaxApiClient maxApiClient;
 @Mock
    private ContextRepository contextRepository;

    private SearchHandler handler;
    private final String botName = "test_bot";

    @BeforeEach
    void setUp() {
        handler = new SearchHandler(botName, patentService, maxApiClient, contextRepository);
    }

    @Test
    void shouldReturnNullAndSendErrorMessageWhenQueryIsEmpty() {
        // Given
        UserContext ctx = new UserContext();
        ctx.setChatId("123");
        ctx.setSearchQuery(""); // Пустой запрос
        BotEvent event = new BotEvent();

        when(maxApiClient.sendMessage(eq(123), any(BotResponse.class))).thenReturn(Mono.empty());

        // When
        BotResponse result = handler.handle(ctx, event);

        // Then
        assertNull(result);
        ArgumentCaptor<BotResponse> responseCaptor = ArgumentCaptor.forClass(BotResponse.class);
        verify(maxApiClient, timeout(1000)).sendMessage(eq(123), responseCaptor.capture());
        assertEquals("❌ Поисковый запрос пуст.", responseCaptor.getValue().getText());
    }

    @Test
    void shouldSendEmptyResultMessageWhenNoPatentsFound() {
        // Given
        UserContext ctx = createBaseContext();
        BotEvent event = new BotEvent();

        PatentSearchResponse emptyResponse = new PatentSearchResponse();
        emptyResponse.setHits(Collections.emptyList());

        when(patentService.searchPatents(any(PatentSearchRequest.class))).thenReturn(Mono.just(emptyResponse));
        when(maxApiClient.sendMessage(eq(123), any(BotResponse.class))).thenReturn(Mono.empty());

        // When
        BotResponse result = handler.handle(ctx, event);

        // Then
        assertNull(result);
        ArgumentCaptor<BotResponse> responseCaptor = ArgumentCaptor.forClass(BotResponse.class);
        verify(maxApiClient, timeout(1000)).sendMessage(eq(123), responseCaptor.capture());
        assertTrue(responseCaptor.getValue().getText().contains("ничего не найдено."));
    }

    @Test
    void shouldFallbackToFallbackTitleAndDateWhenFieldsAreNull() {
        // Given
        UserContext ctx = createBaseContext();
        BotEvent event = new BotEvent();

        PatentHit thinHit = new PatentHit();
        thinHit.setId("RU_EMPTY"); // Все остальные поля null

        PatentSearchResponse searchResponse = new PatentSearchResponse();
        searchResponse.setHits(List.of(thinHit));
        searchResponse.setTotal(0); // Проверка calculateTotalFound ветки hits.size()

        when(patentService.searchPatents(any(PatentSearchRequest.class))).thenReturn(Mono.just(searchResponse));
        when(maxApiClient.sendMessage(eq(123), any(BotResponse.class))).thenReturn(Mono.empty());

        // When
        handler.handle(ctx, event);

        // Then
        ArgumentCaptor<BotResponse> responseCaptor = ArgumentCaptor.forClass(BotResponse.class);
        verify(maxApiClient, timeout(1000).times(3)).sendMessage(eq(123), responseCaptor.capture());

        BotResponse card = responseCaptor.getAllValues().get(1);
        assertFalse(card.getText().contains("Без названия"));
        assertFalse(card.getText().contains("Дата публикации: Не указана"));
    }

    private UserContext createBaseContext() {
        UserContext ctx = new UserContext();
        ctx.setChatId("123");
        ctx.setSearchQuery("Нейросети");
        ctx.setLimit(5);
        ctx.setOffset(0);
        ctx.setDate("20260101");
        ctx.setDatasetArrays(List.of("cis"));
        ctx.setClassifiers("F02K9/00");
        return ctx;
    }
}
