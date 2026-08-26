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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

        PatentHit mockHit = Mockito.mock(PatentHit.class);
        PatentHit.Biblio mockBiblio = Mockito.mock(PatentHit.Biblio.class);

        Mockito.when(mockHit.getId()).thenReturn("RU_EMPTY");
        Mockito.when(mockHit.getBiblio()).thenReturn(mockBiblio);
        Mockito.when(mockBiblio.getRu())
                .thenReturn(null); // Это заставит if сработать как false и уйти на "Без названия"

        PatentSearchResponse searchResponse = new PatentSearchResponse();
        searchResponse.setHits(List.of(mockHit));
        searchResponse.setTotal(1);

        when(patentService.searchPatents(any(PatentSearchRequest.class))).thenReturn(Mono.just(searchResponse));
        when(maxApiClient.sendMessage(eq(123), any(BotResponse.class))).thenReturn(Mono.empty());

        // When
        handler.handle(ctx, event);

        // Then
        ArgumentCaptor<BotResponse> responseCaptor = ArgumentCaptor.forClass(BotResponse.class);
        verify(maxApiClient, timeout(1000).atLeastOnce()).sendMessage(eq(123), responseCaptor.capture());

        List<BotResponse> allResponses = responseCaptor.getAllValues();

        boolean hasDefaultTitle = allResponses.stream()
                .map(BotResponse::getText)
                .anyMatch(text -> text != null && text.contains("Без названия"));

        assertTrue(hasDefaultTitle, "Среди отправленных сообщений должна быть карточка с текстом 'Без названия'");
    }


    @Test
    void shouldSendErrorMessageWhenPatentServiceThrowsExceptionWithReason() {
        // Given
        UserContext ctx = createBaseContext(); // chatId = "123"
        BotEvent event = new BotEvent();

        String errorMessage = "Сервер Роспатента временно недоступен.";
        when(patentService.searchPatents(any(PatentSearchRequest.class)))
                .thenReturn(Mono.error(new RuntimeException(errorMessage)));

        when(maxApiClient.sendMessage(eq(123), any(BotResponse.class))).thenReturn(Mono.empty());

        // When
        BotResponse result = handler.handle(ctx, event);

        // Then
        assertNull(result); // Метод handle всегда возвращает null в конце из-за асинхронного .subscribe()

        ArgumentCaptor<BotResponse> responseCaptor = ArgumentCaptor.forClass(BotResponse.class);
        verify(maxApiClient, timeout(1000)).sendMessage(eq(123), responseCaptor.capture());

        BotResponse errorResponse = responseCaptor.getValue();
        assertNotNull(errorResponse);
        assertTrue(errorResponse.getText().contains("Ошибка обращения к Роспатенту!"));
        assertTrue(errorResponse.getText().contains(errorMessage), "Текст ответа должен содержать причину ошибки");

        assertNotNull(errorResponse.getAttachments());
        assertEquals("BACK_TO_START", errorResponse.getAttachments().get(0).getPayload().getButtons().get(0).get(0)
                .getPayload());
    }

    @Test
    void shouldSendErrorMessageWithFallbackReasonWhenExceptionMessageIsNull() {
        // Given
        UserContext ctx = createBaseContext();
        BotEvent event = new BotEvent();

        when(patentService.searchPatents(any(PatentSearchRequest.class)))
                .thenReturn(Mono.error(new NullPointerException()));

        when(maxApiClient.sendMessage(eq(123), any(BotResponse.class))).thenReturn(Mono.empty());

        // When
        handler.handle(ctx, event);

        // Then
        ArgumentCaptor<BotResponse> responseCaptor = ArgumentCaptor.forClass(BotResponse.class);
        verify(maxApiClient, timeout(1000)).sendMessage(eq(123), responseCaptor.capture());

        BotResponse errorResponse = responseCaptor.getValue();
        assertTrue(errorResponse.getText().contains("Неизвестная ошибка платформы"),
                "При null message должен подставиться дефолтный текст ошибки. Было: " + errorResponse.getText());
    }
    @Test
    void shouldIncludePrevPageButtonWhenOffsetIsGreaterThanZero() {
        // Given: offset = 5, limit = 5, total = 5 (5 > 0 -> сработает только кнопка Назад)
        UserContext ctx = createBaseContext();
        ctx.setOffset(5);
        ctx.setLimit(5);
        BotEvent event = new BotEvent();

        PatentSearchResponse searchResponse = new PatentSearchResponse();
        searchResponse.setHits(List.of(new PatentHit()));
        searchResponse.setTotal(5);

        when(patentService.searchPatents(any(PatentSearchRequest.class))).thenReturn(Mono.just(searchResponse));
        when(maxApiClient.sendMessage(eq(123), any(BotResponse.class))).thenReturn(Mono.empty());

        // When
        handler.handle(ctx, event);

        // Then
        ArgumentCaptor<BotResponse> responseCaptor = ArgumentCaptor.forClass(BotResponse.class);
        verify(maxApiClient, timeout(1000).atLeastOnce()).sendMessage(eq(123), responseCaptor.capture());

        BotResponse menuResponse = responseCaptor.getAllValues().stream()
                .filter(r -> r.getAttachments() != null && !r.getAttachments().isEmpty())
                .filter(r -> r.getAttachments().get(0).getPayload() != null)
                .filter(r -> r.getAttachments().get(0).getPayload().getButtons().stream()
                        .flatMap(List::stream)
                        .anyMatch(b -> "BACK_TO_START".equals(b.getPayload())))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Сервисное меню навигации не найдено"));

        List<List<BotResponse.Button>> buttons = menuResponse.getAttachments().get(0).getPayload().getButtons();

        // Проверяем, что кнопка "Назад" присутствует
        boolean hasPrevButton = buttons.stream()
                .flatMap(List::stream)
                .anyMatch(b -> "SEARCH_PREV_PAGE".equals(b.getPayload()));

        assertTrue(hasPrevButton, "В навигационном меню должна присутствовать кнопка SEARCH_PREV_PAGE");
    }

    @Test
    void shouldIncludeNextPageButtonWhenMoreResultsExist() {
        // Given: offset = 0, limit = 5, total = 10 (0 + 5 < 10 -> сработает кнопка Вперёд)
        UserContext ctx = createBaseContext();
        ctx.setOffset(0);
        ctx.setLimit(5);
        BotEvent event = new BotEvent();

        PatentSearchResponse searchResponse = new PatentSearchResponse();
        searchResponse.setHits(List.of(new PatentHit()));
        searchResponse.setTotal(10);

        when(patentService.searchPatents(any(PatentSearchRequest.class))).thenReturn(Mono.just(searchResponse));
        when(maxApiClient.sendMessage(eq(123), any(BotResponse.class))).thenReturn(Mono.empty());

        // When
        handler.handle(ctx, event);

        // Then
        ArgumentCaptor<BotResponse> responseCaptor = ArgumentCaptor.forClass(BotResponse.class);
        verify(maxApiClient, timeout(1000).atLeastOnce()).sendMessage(eq(123), responseCaptor.capture());

        BotResponse menuResponse = responseCaptor.getAllValues().stream()
                .filter(r -> r.getAttachments() != null && !r.getAttachments().isEmpty())
                .filter(r -> r.getAttachments().get(0).getPayload() != null)
                .filter(r -> r.getAttachments().get(0).getPayload().getButtons().stream()
                        .flatMap(List::stream)
                        .anyMatch(b -> "BACK_TO_START".equals(b.getPayload())))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Сервисное меню навигации не найдено"));

        List<List<BotResponse.Button>> buttons = menuResponse.getAttachments().get(0).getPayload().getButtons();

        // Проверяем, что кнопка "Вперёд" присутствует
        boolean hasNextButton = buttons.stream()
                .flatMap(List::stream)
                .anyMatch(b -> "SEARCH_NEXT_PAGE".equals(b.getPayload()));

        assertTrue(hasNextButton, "В навигационном меню должна присутствовать кнопка SEARCH_NEXT_PAGE");
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
