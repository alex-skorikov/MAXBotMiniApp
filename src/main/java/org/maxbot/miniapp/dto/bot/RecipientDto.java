package org.maxbot.miniapp.dto.bot;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecipientDto {
    private int chatId;
    private String chatType;
    private int userId;

}
