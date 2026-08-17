package org.maxbot.miniapp.dto.patent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatentSearchRequest {

    @JsonProperty("qn")
    private String queryMode;
    @JsonProperty("q")
    private String query;
    private Integer limit;
    private Integer offset;
    private Filter filter;
    @JsonProperty("datasets")
    private List<String> datasets;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Filter {
        @JsonProperty("classification.ipc")
        private Classification classification;
        @JsonProperty("date_published")
        private DatePublished datePublished;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Classification {
        private List<String> values;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DatePublished {
        private Range range;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Range {
        private String gt;
    }
}
