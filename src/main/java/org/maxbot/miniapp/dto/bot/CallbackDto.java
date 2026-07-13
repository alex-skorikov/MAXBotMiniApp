package org.maxbot.miniapp.dto.bot;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CallbackDto {
    private String callbackId;
    private String payload;
    private int userId;

}

