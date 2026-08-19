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
    void shouldSendHeaderCardsAndNavigationWhenPatentsExistWithNextAndPrevPages() {
        // Given
        UserContext ctx = createBaseContext();
        ctx.setSearchLimit(1);
        ctx.setSearchOffset(1); // Имитируем вторую страницу для вызова кнопки "Назад"

        BotEvent event = new BotEvent();

        PatentHit hit = new PatentHit();
        hit.setId("RU123");
        PatentHit.Common common = new PatentHit.Common();
        common.setPublicationDate("2026-08-17");
        hit.setCommon(common);

        PatentHit.Biblio biblio = new PatentHit.Biblio();
        PatentHit.BiblioLang biblioLang = new PatentHit.BiblioLang();
        biblioLang.setTitle("Тестовый Патент");
        biblio.setRu(biblioLang);
        hit.setBiblio(biblio);

        PatentSearchResponse searchResponse = new PatentSearchResponse();
        searchResponse.setHits(List.of(hit));
        searchResponse.setTotal(10); // total (10) > offset(1) + limit(1) -> вызовет кнопку "Вперёд"

        when(patentService.searchPatents(any(PatentSearchRequest.class))).thenReturn(Mono.just(searchResponse));
        when(maxApiClient.sendMessage(eq(123), any(BotResponse.class))).thenReturn(Mono.empty());

        // When
        handler.handle(ctx, event);

        // Then
        // Должно быть отправлено 4 сообщения: заголовок -> карточка патента -> меню навигации
        ArgumentCaptor<BotResponse> responseCaptor = ArgumentCaptor.forClass(BotResponse.class);
        verify(maxApiClient, timeout(1000).times(3)).sendMessage(eq(123), responseCaptor.capture());

        List<BotResponse> sentResponses = responseCaptor.getAllValues();

        // 1. Проверка заголовка
        assertTrue(sentResponses.get(0).getText().contains("Результаты поиска по запросу"));

        // 2. Проверка карточки патента
        assertTrue(sentResponses.get(1).getText().contains("2.Тестовый Патент"));
        assertTrue(sentResponses.get(1).getText().contains("RU123"));

        // 3. Проверка меню навигации
        BotResponse navMenu = sentResponses.get(2);
        assertTrue(navMenu.getText().contains("Управление поиском"));

        List<List<BotResponse.Button>> buttons = navMenu.getAttachments().get(0).getPayload().getButtons();

        // Первая строка должна содержать обе кнопки навигации
        List<BotResponse.Button> pageRow = buttons.get(0);
        assertEquals("⬅️ Назад", pageRow.get(0).getText());
        assertEquals("Вперёд ➡️", pageRow.get(1).getText());

        // Вторая строка — ссылка и сброс
        List<BotResponse.Button> actionRow = buttons.get(1);
        assertEquals("⚙️ Расширенный поиск", actionRow.get(0).getText());
        assertEquals("https://max.ru/test_bot?startapp", actionRow.get(0).getUrl());
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
        ctx.setSearchLimit(5);
        ctx.setSearchOffset(0);
        ctx.setDate("20260101");
        ctx.setSearchArrays(List.of("cis"));
        ctx.setClassifiers("F02K9/00");
        return ctx;
    }
}
