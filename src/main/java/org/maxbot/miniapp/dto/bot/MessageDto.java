package org.maxbot.miniapp.dto.bot;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageDto {
    private RecipientDto recipient;
    private SenderDto sender;
    private BodyDto body;
    private long timestamp;

}

