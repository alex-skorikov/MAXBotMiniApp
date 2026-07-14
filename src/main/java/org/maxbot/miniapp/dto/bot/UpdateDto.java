package org.maxbot.miniapp.dto.bot;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UpdateDto {

    private long timestamp;

    @JsonProperty("user_locale")
    private String userLocale;

    @JsonProperty("update_type")
    private String updateType;

    private MessageDto message;
    private CallbackDto callback;

    @JsonProperty("chat_id")
    private int chatId;

    @JsonProperty("user_id")
    private int userId;
}
