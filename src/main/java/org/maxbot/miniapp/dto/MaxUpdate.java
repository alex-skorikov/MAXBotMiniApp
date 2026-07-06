package org.maxbot.miniapp.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class MaxUpdate {
    private String updateId;
    private MaxMessage message;

    public MaxUpdate(String updateId, MaxMessage message) {
        this.updateId = updateId;
        this.message = message;
    }
}
