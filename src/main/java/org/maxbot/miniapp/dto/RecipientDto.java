package org.maxbot.miniapp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecipientDto {
    private int chat_id;
    private String chat_type;
    private int user_id;

}
