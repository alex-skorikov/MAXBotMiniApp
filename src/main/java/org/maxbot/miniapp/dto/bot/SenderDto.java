package org.maxbot.miniapp.dto.bot;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SenderDto {

    @JsonProperty("user_id")
    private int userId;
    @JsonProperty("first_name")
    private String firstName;
    @JsonProperty("last_name")
    private String lastName;
    @JsonProperty("is_bot")
    private boolean isBot;
    private String name;
    @JsonProperty("last_activity_time")
    private long lastActivityTime;

}
