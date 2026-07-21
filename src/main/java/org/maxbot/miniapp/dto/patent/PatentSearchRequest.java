package org.maxbot.miniapp.dto.patent;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatentSearchRequest {

    private String queryMode;
    private String query;
    private Integer limit;
    private Integer offset;

}
