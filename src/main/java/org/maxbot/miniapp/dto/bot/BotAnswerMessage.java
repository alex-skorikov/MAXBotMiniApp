package org.maxbot.miniapp.dto.bot;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class BotAnswerMessage {

    private final String text;
    private List<Attachment> attachments;

    public BotAnswerMessage(String text,
                            List<Attachment> attachments) {
        this.text = text;
        this.attachments = attachments;
    }

    // ===== Attachment =====
    @Builder
    @Getter
    @Setter
    public static class Attachment {
        private String type;
        private InlineKeyboardPayload payload;

        public Attachment(String type, InlineKeyboardPayload payload) {
            this.type = type;
            this.payload = payload;
        }

    }

    // ===== Payload =====
    @Builder
    @Getter
    @Setter
    public static class InlineKeyboardPayload {
        private List<List<Button>> buttons;

        public InlineKeyboardPayload(List<List<Button>> buttons) {
            this.buttons = buttons;
        }

    }

    // ===== Button =====
    @Builder
    @Getter
    @Setter
    public static class Button {
        private String type;
        private String text;
        private String url;

        public Button(String type, String text, String url) {
            this.type = type;
            this.text = text;
            this.url = url;
        }

    }

}
