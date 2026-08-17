package org.maxbot.miniapp.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.client.RospatentClient;
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
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatentServiceTest {

    @Mock
    private MaxApiClient maxApiClient;

    @Mock
    private RospatentClient rospatentClient;

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

        PatentSearchResponse emptyResponse = new PatentSearchResponse();
        emptyResponse.setHits(Collections.emptyList());

        // Мокаем вызов к Роспатенту (поиск вернет пустой список)
        when(rospatentClient.searchReactive(any(PatentSearchRequest.class)))
                .thenReturn(Mono.just(emptyResponse));

        // Мокаем отправку сообщения об ошибке через апи клиента
        when(maxApiClient.sendMessage(eq(chatId), any(BotResponse.class)))
                .thenReturn(Mono.empty());

        // When
        patentService.sendSinglePatentCardAsync(chatId, docId);

        // Then
        // Поскольку метод асинхронный и работает на Schedulers.boundedElastic(),
        // используем timeout() в verify, чтобы дождаться асинхронного вызова.
        ArgumentCaptor<BotResponse> responseCaptor = ArgumentCaptor.forClass(BotResponse.class);
        verify(maxApiClient, timeout(1000).times(1)).sendMessage(eq(chatId), responseCaptor.capture());

        BotResponse sentResponse = responseCaptor.getValue();
        assertFalse(sentResponse.isNotify());
        assertTrue(sentResponse.getText().contains("❌ Не удалось загрузить информацию по документу " + docId));
    }

    @Test
    void shouldSendFormattedPatentCardAsyncWhenPatentExists() {
        // Given
        int chatId = 555;
        String docId = "RU/2147483/C1"; // Используем спецсимволы для проверки URLEncoder

        // Строим структуру ответа, чтобы formatPatentCard внутри не падал на NPE
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
        common.setPublishingOffice("RU");
        common.setDocumentNumber("2147483");
        common.setKind("C1");
        common.setPublicationDate("2026-08-17");
        hit.setCommon(common);

        PatentHit.Snippet snippet = new PatentHit.Snippet();
        snippet.setDescription("Описание...");
        hit.setSnippet(snippet);

        PatentSearchResponse mockResponse = new PatentSearchResponse();
        mockResponse.setHits(List.of(hit));

        when(rospatentClient.searchReactive(any(PatentSearchRequest.class)))
                .thenReturn(Mono.just(mockResponse));
        when(maxApiClient.sendMessage(eq(chatId), any(BotResponse.class)))
                .thenReturn(Mono.empty());

        // When
        patentService.sendSinglePatentCardAsync(chatId, docId);

        // Then
        ArgumentCaptor<BotResponse> responseCaptor = ArgumentCaptor.forClass(BotResponse.class);
        verify(maxApiClient, timeout(1000).times(1)).sendMessage(eq(chatId), responseCaptor.capture());

        BotResponse sentResponse = responseCaptor.getValue();
        assertFalse(sentResponse.isNotify());
        assertTrue(sentResponse.getText().contains("📄 Супер Двигатель"));

        // Проверяем инлайн-кнопку с экранированной ссылкой
        assertNotNull(sentResponse.getAttachments());
        BotResponse.Attachment attachment = sentResponse.getAttachments().get(0);
        assertEquals("inline_keyboard", attachment.getType());

        BotResponse.Button button = attachment.getPayload().getButtons().get(0).get(0);
        assertEquals("link", button.getType());
    }

    @Test
    void shouldHandleErrorAndLogWhenSearchFails() {
        // Given
        int chatId = 999;
        String docId = "FAIL_ID";

        // Симулируем выброс ошибки реактивным стримом клиента
        when(rospatentClient.searchReactive(any(PatentSearchRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Роспатент недоступен")));

        // When
        patentService.sendSinglePatentCardAsync(chatId, docId);

        // Then
        // Проверяем, что из-за doOnError метод не упал, а обработка завершилась.
        // Сообщение в макс клиент при этом уйти не должно.
        verify(maxApiClient, after(500).never()).sendMessage(anyInt(), any());
    }
}
