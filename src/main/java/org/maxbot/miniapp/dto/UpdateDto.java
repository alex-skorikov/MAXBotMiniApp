package org.maxbot.miniapp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDto {
    private String update_type;
    private long timestamp;
    private MessageDto message;
    private String user_locale;

}

