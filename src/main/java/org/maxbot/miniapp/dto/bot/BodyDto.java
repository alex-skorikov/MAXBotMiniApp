package org.maxbot.miniapp.dto.bot;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class BodyDto {
    private String text;
    private String mid;
    private long seq;
    private String payload;

}
