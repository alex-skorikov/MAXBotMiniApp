package org.maxbot.miniapp.stepalgo;

import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.dto.bot.BotAnswerMessage;
import org.maxbot.miniapp.dto.bot.CallbackDto;
import org.maxbot.miniapp.dto.bot.MessageDto;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import org.maxbot.miniapp.dto.patent.PatentSearchRequest;
import org.maxbot.miniapp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BotStepsAlgo extends AbstractAlgo {
    private static final Logger log = LoggerFactory.getLogger(BotStepsAlgo.class);
    private final Map<Integer, PatentSearchRequest> userState = new ConcurrentHashMap<>();
    private final MaxApiClient maxApiClient;

    private final String nameAlgo;
    private CallbackDto cb;
    private int userId;
    private int chatId;

    public BotStepsAlgo(MaxApiClient maxApiClient,
                        String nameAlgo) {
        this.maxApiClient = maxApiClient;
        this.nameAlgo = nameAlgo;
    }

    @Override
    protected int maxStepNumber() {
        return 8;
    }

    //--- Search ---
    @Override
    public AlgoStatus step0(UpdateDto upd) {
        log.info("{} Start step_0", nameAlgo);
        return AlgoStatus.STEP_1;
    }

    //--- Старт бота, Любое сообщение - отправляем меню с базами ---
    @Override
    public AlgoStatus step1(UpdateDto upd) {
        log.info("{} Start step_1", nameAlgo);
        cb = upd.getCallback();
        userId = cb.getUser().getUserId();
        chatId = upd.getMessage().getRecipient().getChatId();

        if ("bot_started".equals(upd.getUpdateType()) || "message_created".equals(upd.getUpdateType())) {
            String text = "Добро пожаловать!" + "\n" + "Выберите базу:";
            List<List<BotAnswerMessage.Button>> buttons = getButtons(Map.of(
                    "Патенты", "PATENTS",
                    "Промобразцы", "PROM_SAMPLE",
                    "Полезные модели", "MODELS"));
            maxApiClient.sendMenu(upd.getChatId(), text, buttons)
                    .onErrorResume(e -> Mono.empty());

            userState.put(upd.getChatId(), new PatentSearchRequest());

            return AlgoStatus.END;
        }
        return AlgoStatus.STEP_2;
    }

    //--- Выбор базы ---
    @Override
    public AlgoStatus step2(UpdateDto upd) {
        log.info("{} Start step_2", nameAlgo);
        String payload = cb.getPayload();
        if ("message_callback".equals(upd.getUpdateType())) {
            PatentSearchRequest patentSearchRequest = userState.get(userId);
            switch (payload) {
                case "PATENTS":
                    patentSearchRequest.setFilter("PATENTS");
                    return AlgoStatus.STEP_3;
                case "PROM_SAMPLE":
                    patentSearchRequest.setFilter("PROM_SAMPLE");
                    return AlgoStatus.STEP_3;
                case "MODELS":
                    patentSearchRequest.setFilter("MODELS");
                    return AlgoStatus.STEP_3;
            }
        }
        return AlgoStatus.STEP_4;
    }

    //--- Выбрана база, меню фильтров ---
    @Override
    public AlgoStatus step3(UpdateDto upd) {
        log.info("{} Start step_3", nameAlgo);
        String text = "Выберите фильтры:" + "\n";
        List<List<BotAnswerMessage.Button>> buttons = getButtons(Map.of(
                "Дата", "DATE",
                "Поисковые массивы", "SEARCH_ARRAY",
                "Классификаторы", "CLASSIFIERS",
                "Назад", "BACK"));
        maxApiClient.sendBaseAnswer(chatId, text, buttons)
                .onErrorResume(e -> Mono.empty());
        return AlgoStatus.STEP_4;
    }

    //--- Выбраны фильтры ---
    @Override
    public AlgoStatus step4(UpdateDto upd) {
        log.info("{} Start step_4", nameAlgo);
        String payload = cb.getPayload();
        if ("message_callback".equals(upd.getUpdateType())) {
            PatentSearchRequest patentSearchRequest = userState.get(userId);
            switch (payload) {
                case "DATE":
                    patentSearchRequest.setFilter("DATE");
                    return AlgoStatus.STEP_5;
                case "SEARCH_ARRAY":
                    patentSearchRequest.setFilter("SEARCH_ARRAY");
                    return AlgoStatus.STEP_6;
                case "CLASSIFIERS":
                    patentSearchRequest.setFilter("CLASSIFIERS");
                    return AlgoStatus.STEP_7;
                case "BACK":
                    return AlgoStatus.STEP_1;
            }
        }
        return AlgoStatus.STEP_4;
    }

    //--- Выбраны ---
    @Override
    public AlgoStatus step5(UpdateDto upd) {
        log.info("{} Start step_5", nameAlgo);
//        userState.put(userId, "PATENT_SEARCH");
        BotAnswerMessage searchRq = BotAnswerMessage.builder()
                .text("Введите Дату в формате 2020-01-01:")
                .build();
        maxApiClient.sendMessage(chatId, searchRq);
        return AlgoStatus.END;
    }


    private List<List<BotAnswerMessage.Button>> getButtons(Map<String, String> objectMap) {

        List<BotAnswerMessage.Button> list = new ArrayList<>();
        objectMap.forEach((k, v) -> {
            BotAnswerMessage.Button callback = BotAnswerMessage.Button.builder()
                    .type("callback")
                    .text(k)
                    .payload(v)
                    .build();
            list.add(callback);
        });
        return List.of(list);
    }
}
