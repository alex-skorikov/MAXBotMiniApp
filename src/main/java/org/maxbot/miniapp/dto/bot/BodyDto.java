package org.maxbot.miniapp.dto.bot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class BodyDto {
    private String text;
    private String mid;
    private long seq;
    //    private String payload;
    @JsonIgnore
    private List<Object> attachments;

}
