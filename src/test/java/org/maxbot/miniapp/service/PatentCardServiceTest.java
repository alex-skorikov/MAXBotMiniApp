package org.maxbot.miniapp.service;

import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.dto.patent.PatentHit;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatentCardServiceTest {

    @Test
    void shouldFormatPatentCardWithShortDescriptionSuccessfully() {
        // Given
        PatentHit card = createBasePatentHit("Короткое описание патента.");

        // When
        String result = PatentCardService.formatPatentCard(card);

        // Then
        String expected = "📄 Тестовый патент на двигатель\n" +
                "МПК: МПК-1, МПК-2\n" +
                "Документ: RU 2147483 C1 (20260817)\n" +
                "Заявители:\n" +
                "• ООО Рога и Копыта\n" +
                "• Сколково\n\n" +
                "Авторы:\n" +
                "• Иванов И.И.\n" +
                "• Петров П.П.\n\n" +
                "Описание:\n" +
                "Короткое описание патента.\n";

        assertEquals(expected, result);
    }

    @Test
    void shouldTruncateDescriptionWhenItExceeds300Characters() {
        // Given
        // Создаем строку ровно в 310 символов
        String longDescription = "А".repeat(310);
        PatentHit card = createBasePatentHit(longDescription);

        // When
        String result = PatentCardService.formatPatentCard(card);

        // Then
        // Ожидаем 300 символов 'А' и знак многоточия '…'
        String expectedDescriptionSnippet = "Описание:\n" + "А".repeat(300) + "…\n";
        assertTrue(result.contains(expectedDescriptionSnippet));
    }

    @Test
    void shouldFormatPatentCardWhenListsAreEmpty() {
        // Given
        PatentHit card = new PatentHit();

        PatentHit.Biblio biblio = new PatentHit.Biblio();
        PatentHit.BiblioLang biblioLang = new PatentHit.BiblioLang();
        biblioLang.setTitle("Пустой патент");
        biblioLang.setApplicant(Collections.emptyList());
        biblioLang.setInventor(Collections.emptyList());
        biblio.setRu(biblioLang);
        card.setBiblio(biblio);

        PatentHit.Common common = new PatentHit.Common();
        PatentHit.Classification classification = new PatentHit.Classification();
        classification.setIpc(Collections.emptyList());
        common.setClassification(classification);
        common.setPublishingOffice("RU");
        common.setDocumentNumber("123");
        common.setKind("A");
        common.setPublicationDate("2026");
        card.setCommon(common);

        PatentHit.Snippet snippet = new PatentHit.Snippet();
        snippet.setDescription("Тест");
        card.setSnippet(snippet);

        // When
        String result = PatentCardService.formatPatentCard(card);

        // Then
        assertTrue(result.contains("МПК: \n"));
        assertTrue(result.contains("Заявители:\n\n"));
        assertTrue(result.contains("Авторы:\n\n"));
    }

    // Хелпер для быстрой сборки заполненного PatentHit
    private PatentHit createBasePatentHit(String description) {
        PatentHit card = new PatentHit();

        // 1. Библиография (Заголовок, Заявители, Авторы)
        PatentHit.Biblio biblio = new PatentHit.Biblio();
        PatentHit.BiblioLang biblioLang = new PatentHit.BiblioLang();

        biblioLang.setTitle("Тестовый патент на двигатель");

        PatentHit.NameWrapper app1 = new PatentHit.NameWrapper();
        app1.setName("ООО Рога и Копыта");
        PatentHit.NameWrapper app2 = new PatentHit.NameWrapper();
        app2.setName("Сколково");
        biblioLang.setApplicant(List.of(app1, app2));

        PatentHit.NameWrapper inv1 = new PatentHit.NameWrapper();
        inv1.setName("Иванов И.И.");
        PatentHit.NameWrapper inv2 = new PatentHit.NameWrapper();
        inv2.setName("Петров П.П.");
        biblioLang.setInventor(List.of(inv1, inv2));

        biblio.setRu(biblioLang);
        card.setBiblio(biblio);

        // 2. Общие данные (МПК, Номера)
        PatentHit.Common common = new PatentHit.Common();
        PatentHit.Classification classification = new PatentHit.Classification();

        PatentHit.IpcItem ipc1 = new PatentHit.IpcItem();
        ipc1.setFullname("МПК-1");
        PatentHit.IpcItem ipc2 = new PatentHit.IpcItem();
        ipc2.setFullname("МПК-2");
        classification.setIpc(List.of(ipc1, ipc2));

        common.setClassification(classification);
        common.setPublishingOffice("RU");
        common.setDocumentNumber("2147483");
        common.setKind("C1");
        common.setPublicationDate("20260817");
        card.setCommon(common);

        // 3. Аннотация / Сниппет
        PatentHit.Snippet snippet = new PatentHit.Snippet();
        snippet.setDescription(description);
        card.setSnippet(snippet);

        return card;
    }
}
