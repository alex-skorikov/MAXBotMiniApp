package org.maxbot.miniapp.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.client.RosPatentClient;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.maxbot.miniapp.dto.patent.PatentHit;
import org.maxbot.miniapp.dto.patent.PatentSearchRequest;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatentServiceTest {

    @Mock
    private MaxApiClient maxApiClient;

    @Mock
    private RosPatentClient rospatentClient;

    @InjectMocks
    private PatentService patentService;

    @Test
    void shouldReturnSearchResponseWhenSearchingPatents() {
        // Given
        PatentSearchRequest request = new PatentSearchRequest();
        PatentSearchResponse mockResponse = new PatentSearchResponse();

        when(rospatentClient.searchReactive(request)).thenReturn(Mono.just(mockResponse));

        // When
        Mono<PatentSearchResponse> result = patentService.searchPatents(request);

        // Then
        StepVerifier.create(result)
                .expectNext(mockResponse)
                .verifyComplete();
    }

    @Test
    void shouldSendErrorCardAsyncWhenPatentNotFoundOrHitsEmpty() {
        // Given
        int chatId = 777;
        String docId = "RU12345";

        // Мокаем отправку сообщения через апи клиента
        when(maxApiClient.sendMessage(eq(chatId), any(BotResponse.class)))
                .thenReturn(Mono.empty());

        // Передаем пустой контекст (патент не будет найден)
        UserContext userContext = new UserContext();
        userContext.setHits(List.of());

        // When
        patentService.sendSinglePatentCardAsync(chatId, docId, userContext);

        // Then
        ArgumentCaptor<BotResponse> responseCaptor = ArgumentCaptor.forClass(BotResponse.class);
        // Так как метод больше не реактивный (работает синхронно), timeout() больше не нужен!
        verify(maxApiClient, times(1)).sendMessage(eq(chatId), responseCaptor.capture());

        BotResponse sentResponse = responseCaptor.getValue();
        assertFalse(sentResponse.isNotify());
        assertTrue(sentResponse.getText().contains("❌ Не удалось загрузить информацию по документу " + docId));
    }

    @Test
    void shouldSendFormattedPatentCardAsyncWhenPatentExists() {
        // Given
        int chatId = 555;
        String docId = "RU_2147483_C1"; // Избегаем слэшей, чтобы URLEncoder отработал предсказуемо

        // Строим объект патента
        PatentHit hit = new PatentHit();
        hit.setId(docId);

        PatentHit.Biblio biblio = new PatentHit.Biblio();
        PatentHit.BiblioLang biblioLang = new PatentHit.BiblioLang();
        biblioLang.setTitle("Супер Двигатель");
        biblioLang.setApplicant(Collections.emptyList());
        biblioLang.setInventor(Collections.emptyList());
        biblio.setRu(biblioLang);
        hit.setBiblio(biblio);

        PatentHit.Common common = new PatentHit.Common();
        PatentHit.Classification classification = new PatentHit.Classification();
        classification.setIpc(Collections.emptyList());
        common.setClassification(classification);
        common.setPublishingOffice("ru_since_1994"); // Ключ из нашей static final Map
        common.setDocumentNumber("2147483");
        common.setKind("C1");
        common.setPublicationDate("2026-08-17");
        hit.setCommon(common);

        PatentHit.Snippet snippet = new PatentHit.Snippet();
        snippet.setDescription("Описание...");
        hit.setSnippet(snippet);

        // ВАЖНО: Кладем патент в контекст пользователя, чтобы метод его нашел!
        UserContext userContext = new UserContext();
        userContext.setHits(List.of(hit));

        when(maxApiClient.sendMessage(eq(chatId), any(BotResponse.class)))
                .thenReturn(Mono.empty());

        // When
        patentService.sendSinglePatentCardAsync(chatId, docId, userContext);

        // Then
        ArgumentCaptor<BotResponse> responseCaptor = ArgumentCaptor.forClass(BotResponse.class);
        verify(maxApiClient, times(1)).sendMessage(eq(chatId), responseCaptor.capture());

        BotResponse sentResponse = responseCaptor.getValue();
        assertFalse(sentResponse.isNotify());

        // Проверяем инлайн-кнопку со сформированной ссылкой на платформу Роспатента
        assertNotNull(sentResponse.getAttachments());
        BotResponse.Attachment attachment = sentResponse.getAttachments().get(0);
        assertEquals("inline_keyboard", attachment.getType());

        BotResponse.Button button = attachment.getPayload().getButtons().get(0).get(0);
        assertEquals("link", button.getType());
        assertTrue(button.getUrl().contains("https://searchplatform.rospatent.gov.ru/doc/"));
    }


    @Test
    void shouldHandleErrorAndLogWhenSearchFails() {
        // Given
        int chatId = 999;
        String docId = "FAIL_ID";

        when(maxApiClient.sendMessage(eq(chatId), any(BotResponse.class)))
                .thenReturn(Mono.empty());

        // Симулируем критическую ситуацию: передаем null вместо контекста
        UserContext userContext = null;

        // When & Then
        UserContext emptyContext = new UserContext();
        emptyContext.setHits(List.of());

        patentService.sendSinglePatentCardAsync(chatId, docId, emptyContext);

        verify(maxApiClient, times(1)).sendMessage(eq(chatId), any(BotResponse.class));
    }

}
