package org.maxbot.miniapp.dto.bot;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CallbackDto {
    @JsonProperty("callback_id")
    private String callbackId;
    private String payload;
    @JsonProperty("user_id")
    private int userId;

}

