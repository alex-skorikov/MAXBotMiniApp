package org.maxbot.miniapp.dto.patent;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatentSearchRequest {

    private String query;
    private String queryMode; // "q" или "qn"
    private Integer page;
    private Integer pageSize;
    private Integer includeFacets;
    private Integer limit;
    private Integer offset;

}
