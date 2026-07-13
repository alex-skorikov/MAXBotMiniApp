package org.maxbot.miniapp.dto.bot;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CallbackDto {
    private String id;
    private String payload;
    private int user_id;
}
