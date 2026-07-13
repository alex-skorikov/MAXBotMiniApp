package org.maxbot.miniapp.dto.bot;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SenderDto {

    private int userId;
    private String firstName;
    private String lastName;
    private boolean isBot;
    private String name;

}
