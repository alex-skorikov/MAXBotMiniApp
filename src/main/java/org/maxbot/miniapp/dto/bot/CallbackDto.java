package org.maxbot.miniapp.dto.bot;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CallbackDto {
    private String callbackId;
    private String payload;
    private int userId;

}

