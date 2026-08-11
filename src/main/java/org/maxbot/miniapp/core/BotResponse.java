package org.maxbot.miniapp.core;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.maxbot.miniapp.dto.bot.BotAnswerMessage;

import java.util.List;

@Builder
@Getter
@Setter
@ToString
public class BotResponse {


    private final String text;
    private List<BotResponse.Attachment> attachments;
    private String format;

    public BotResponse(String text,
                       List<BotResponse.Attachment> attachments,
                       String format) {
        this.text = text;
        this.attachments = attachments;
        this.format = format;
    }

    // ===== Attachment =====
    @Builder
    @Getter
    @Setter
    @ToString
    public static class Attachment {
        private String type;
        private BotResponse.InlineKeyboardPayload payload;

        public Attachment(String type, BotResponse.InlineKeyboardPayload payload) {
            this.type = type;
            this.payload = payload;
        }

    }

    // ===== Payload =====
    @Builder
    @Getter
    @Setter
    @ToString
    public static class InlineKeyboardPayload {
        private List<List<BotResponse.Button>> buttons;

        public InlineKeyboardPayload(List<List<BotResponse.Button>> buttons) {
            this.buttons = buttons;
        }

    }

    // ===== Button =====
    @Builder
    @Getter
    @Setter
    @ToString
    public static class Button {
        private String type;
        private String text;
        private String url;
        private String payload;

        public Button(String type, String text, String url, String payload) {
            this.type = type;
            this.text = text;
            this.url = url;
            this.payload = payload;
        }

    }
}
