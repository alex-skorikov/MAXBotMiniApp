package org.maxbot.miniapp.dto.bot;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UpdateDto {
    private String update_type;
    private long timestamp;
    private MessageDto message;
    private String user_locale;
    private CallbackDto callback;
}

