package org.maxbot.miniapp.dto.bot;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CallbackDto {
    private String callback_id;
    private String payload;
    private int user_id;

}

