package org.maxbot.miniapp.dto.bot;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RecipientDto {
    @JsonProperty("chat_id")
    private int chatId;
    @JsonProperty("chat_type")
    private String chatType;
    @JsonProperty("user_id")
    private int userId;

}
