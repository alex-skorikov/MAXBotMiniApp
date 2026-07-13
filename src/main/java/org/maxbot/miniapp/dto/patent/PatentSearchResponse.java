package org.maxbot.miniapp.dto.patent;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PatentSearchResponse {
    private int total;
    private int available;
    private List<PatentHit> hits;

}

