package org.maxbot.miniapp.dto.patent;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PatentSearchPagedResponse {

    private List<PatentHit> items;

    private Pagination pagination;

    @Getter
    @Setter
    public static class Pagination {
        private int page;
        private int pageSize;
        private int total;
        private boolean hasNext;

    }
}
