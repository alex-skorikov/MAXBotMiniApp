package org.maxbot.miniapp.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.repository.ContextRepository; // Нужен для сохранения offset
import org.maxbot.miniapp.service.PatentSearchService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchHandler implements StepHandler {

    private final PatentSearchService patentSearchService;
    private final MaxApiClient maxApiClient;
    private final ContextRepository contextRepository;

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {
        String query = ctx.getSearchQuery();
        int chatId = Integer.parseInt(ctx.getChatId());

        // Задаем дефолтные значения пагинации, если они еще не установлены
        int limit = ctx.getSearchLimit() > 0 ? ctx.getSearchLimit() : 5;
        int offset = ctx.getSearchOffset();

        if (query == null || query.isBlank()) {
            sendTextMessageAsync(chatId, "❌ Поисковый запрос пуст.");
            return null;
        }

        patentSearchService.searchReactive("q", query, limit, offset)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(searchResponse -> {
                    if (searchResponse == null || searchResponse.getHits() == null || searchResponse.getHits().isEmpty()) {
                        return maxApiClient.sendMessage(chatId, BotResponse.builder()
                                .text("🔍 По запросу \"" + query + "\" ничего не найдено.")
                                .build());
                    }

                    long totalFound = searchResponse.getTotal();

                    // Шаг 49: Формируем текст "Найдено X документов"
                    StringBuilder textBuilder = new StringBuilder();
                    textBuilder.append("🔍 **Результаты поиска по запросу:** \"").append(query).append("\"\n");
                    textBuilder.append("📊 **Найдено документов:** ").append(totalFound).append("\n\n");
                    textBuilder.append("Выберите документ ниже для просмотра подробной карточки:");

                    List<List<BotResponse.Button>> buttons = new ArrayList<>();

                    // Шаг 50: Формируем инлайн-кнопки для каждого документа [Документ 1], [Документ 2]
                    searchResponse.getHits().forEach(hit -> {
                        String docId = hit.getId(); // Например, "RU123456"
                        buttons.add(List.of(BotResponse.Button.builder()
                                .type("callback")
                                .text("📄 " + docId)
                                .payload("DOC_VIEW_" + docId) // Будет обрабатываться в MaxMapper
                                .build()));
                    });

                    // Шаг 51: Навигационная панель [Назад] [Вперёд] [Расширенный поиск]
                    List<BotResponse.Button> navigationRow = new ArrayList<>();

                    if (offset > 0) {
                        navigationRow.add(BotResponse.Button.builder()
                                .type("callback")
                                .text("⬅️ Назад")
                                .payload("SEARCH_PREV_PAGE")
                                .build());
                    }

                    if (offset + limit < totalFound) {
                        navigationRow.add(BotResponse.Button.builder()
                                .type("callback")
                                .text("Вперёд ➡️")
                                .payload("SEARCH_NEXT_PAGE")
                                .build());
                    }

                    if (!navigationRow.isEmpty()) {
                        buttons.add(navigationRow);
                    }

                    // Кнопка системного управления и расширенного поиска
                    buttons.add(List.of(
                            BotResponse.Button.builder()
                                    .type("callback")
                                    .text("⚙️ Расширенный поиск")
                                    .payload("ADVANCED_SEARCH")
                                    .build(),
                            BotResponse.Button.builder()
                                    .type("callback")
                                    .text("🔄 Сбросить")
                                    .payload("BACK_TO_START")
                                    .build()
                    ));

                    BotResponse resultsMenu = BotResponse.builder()
                            .text(textBuilder.toString())
                            .attachments(List.of(
                                    BotResponse.Attachment.builder()
                                            .type("inline_keyboard")
                                            .payload(BotResponse.InlineKeyboardPayload.builder()
                                                    .buttons(buttons)
                                                    .build())
                                            .build()
                            ))
                            .build();

                    return maxApiClient.sendMessage(chatId, resultsMenu);
                })
                .doOnError(e -> {
                    log.error("Ошибка построения списка документов для чата {}", chatId, e);
                    sendTextMessageAsync(chatId, "❌ Произошла ошибка при выводе списка результатов.");
                })
                .subscribe();

        return null;
    }

    private void sendTextMessageAsync(int chatId, String text) {
        maxApiClient.sendMessage(chatId, BotResponse.builder().text(text).build())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }
}
