package org.maxbot.miniapp.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.service.PatentSearchService;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchHandler implements StepHandler {

    private final PatentSearchService patentSearchService;
    private final MaxApiClient maxApiClient;

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {
        String query = ctx.getSearchQuery();
        int chatId = Integer.parseInt(ctx.getChatId());

        int limit = (ctx.getSearchLimit() > 0) ? ctx.getSearchLimit() : 5;
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
                                        .notify(false)
                                .text("🔍 По запросу \"" + query + "\" ничего не найдено.")
                                .build());
                    }

                    long totalFound = searchResponse.getTotal() != 0 ? searchResponse.getTotal() : searchResponse.getHits().size();

                    StringBuilder textBuilder = new StringBuilder();
                    textBuilder.append("🔍 **Результаты поиска по запросу:** \"").append(query).append("\"\n");
                    textBuilder.append("📊 **Найдено документов:** ").append(totalFound).append("\n\n");
                    textBuilder.append("Выберите интересующий документ для просмотра подробной карточки:");

                    List<List<BotResponse.Button>> buttons = new ArrayList<>();

                    searchResponse.getHits().forEach(hit -> {
                        String docId = hit.getId();
                        buttons.add(List.of(BotResponse.Button.builder()
                                .type("callback")
                                .text("📄 " + docId)
                                .payload("DOC_VIEW_" + docId)
                                .build()));
                    });

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
                            .notify(false)
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
                .doOnError(e -> log.error("Ошибка генерации списка патентов", e))
                .subscribe();

        return null;
    }

    private void sendTextMessageAsync(int chatId, String text) {
        maxApiClient.sendMessage(chatId, BotResponse.builder().notify(false).text(text).build())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }
}
