package org.maxbot.miniapp.dto.bot;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SenderDto {

    private int user_id;
    private String first_name;
    private String last_name;
    private boolean is_bot;
    private String name;

}
