package org.maxbot.miniapp.handlers;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.maxbot.miniapp.service.PatentService;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchHandler implements StepHandler {

    private final PatentService patentService;
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

        patentService.searchReactive("q", query, limit, offset)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(searchResponse -> {
                    if (searchResponse == null || searchResponse.getHits() == null
                            || searchResponse.getHits().isEmpty()) {
                        return maxApiClient.sendMessage(chatId, BotResponse.builder()
                                .notify(false)
                                .text("🔍 По запросу \"" + query + "\" ничего не найдено.")
                                .build());
                    }

                    long totalFound = searchResponse.getTotal() != 0
                            ? searchResponse.getTotal() : searchResponse.getHits().size();

                    StringBuilder textBuilder = new StringBuilder();
                    textBuilder.append("🔍 **Результаты поиска по запросу:** \"").append(query).append("\"\n");
                    textBuilder.append("📊 **Найдено документов:** ").append(totalFound);

                    List<BotResponse.Attachment> attachments = new ArrayList<>();
                    int[] counter = {offset + 1};

                    // 1. СБОРКА КАРТОЧЕК ПАТЕНТОВ ЧЕРЕЗ INLINE_KEYBOARD PAYLOAD TEXT
                    searchResponse.getHits().forEach(hit -> {
                        String publicationDate = hit.getCommon() != null
                                ? hit.getCommon().getPublicationDate() : "Не указана";
                        String docId = hit.getId();

                        String title = "Без названия";
                        if (hit.getBiblio() != null && hit.getBiblio().getRu() != null
                                && hit.getBiblio().getRu().getTitle() != null) {
                            title = hit.getBiblio().getRu().getTitle();
                        }

                        // Текст внутри конкретной плитки-баббла
                        String cardText = String.format("%d. %s\n%s\nДата публикации: %s",
                                counter[0]++, title, docId, publicationDate);

                        List<List<BotResponse.Button>> cardButtons = List.of(List.of(
                                BotResponse.Button.builder()
                                        .type("callback")
                                        .text("Подробнее")
                                        .payload("DOC_VIEW_" + docId)
                                        .build()
                        ));

                        // 🔥 Передаем тип inline_keyboard, но текст кладем внутрь payload
                        attachments.add(BotResponse.Attachment.builder()
                                .type("inline_keyboard")
                                .payload(BotResponse.InlineKeyboardPayload.builder()
                                        .text(cardText) // Текст привяжется к этой конкретной клавиатуре-плитке
                                        .buttons(cardButtons)
                                        .build())
                                .build());
                    });

                    // 2. НИЖНЕЕ МЕНЮ НАВИГАЦИИ (ОСТАЕТСЯ БЕЗ ИЗМЕНЕНИЙ)
                    List<List<BotResponse.Button>> navButtons = new ArrayList<>();
                    List<BotResponse.Button> navigationRow = new ArrayList<>();

                    if (offset > 0) {
                        navigationRow.add(BotResponse.Button.builder().type("callback").text("⬅️ Назад").payload("SEARCH_PREV_PAGE").build());
                    }
                    if (offset + limit < totalFound) {
                        navigationRow.add(BotResponse.Button.builder().type("callback").text("Вперёд ➡️").payload("SEARCH_NEXT_PAGE").build());
                    }
                    if (!navigationRow.isEmpty()) {
                        navButtons.add(navigationRow);
                    }

                    navButtons.add(List.of(
                            BotResponse.Button.builder().type("web_app").text("⚙️ Расширенный поиск").payload("https://vercel.app").build(),
                            BotResponse.Button.builder().type("callback").text("🔄 Сбросить").payload("BACK_TO_START").build()
                    ));

                    attachments.add(BotResponse.Attachment.builder()
                            .type("inline_keyboard")
                            .payload(BotResponse.InlineKeyboardPayload.builder()
                                    .buttons(navButtons) // Тут текста нет, уйдут только кнопки навигации
                                    .build())
                            .build());

                    BotResponse resultsMenu = BotResponse.builder()
                            .notify(false)
                            .text(textBuilder.toString())
                            .attachments(attachments)
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
