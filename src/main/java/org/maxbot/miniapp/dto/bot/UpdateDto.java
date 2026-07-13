package org.maxbot.miniapp.dto.bot;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UpdateDto {
    private String updateType;
    private long timestamp;
    private MessageDto message;
    private String userLocale;
    private CallbackDto callback;
}

