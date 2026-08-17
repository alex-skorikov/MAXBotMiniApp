package org.maxbot.miniapp.util;

import org.maxbot.miniapp.dto.patent.PatentSearchRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatentsUtil {

    public static Map<String, Object> patentRequestToMap(PatentSearchRequest dto) {
        Map<String, Object> body = new HashMap<>();

        String queryMode = dto.getQueryMode() != null ? dto.getQueryMode() : "qn";
        if (dto.getQuery() != null) {
            body.put(queryMode, dto.getQuery());
        }

        if (dto.getLimit() != null) body.put("limit", dto.getLimit());
        if (dto.getOffset() != null) body.put("offset", dto.getOffset());

        if (dto.getDatasets() != null && !dto.getDatasets().isEmpty()) {
            body.put("datasets", dto.getDatasets());
        }

        Map<String, Object> filterMap = new HashMap<>();

        if (dto.getFilter() != null && dto.getFilter().getClassification() != null) {
            List<String> ipcValues = dto.getFilter().getClassification().getValues();
            if (ipcValues != null && !ipcValues.isEmpty()) {
                Map<String, Object> valuesMap = new HashMap<>();
                valuesMap.put("values", ipcValues);
                filterMap.put("classification.ipc", valuesMap);
            }
        }

        if (dto.getFilter() != null && dto.getFilter().getDatePublished() != null
                && dto.getFilter().getDatePublished().getRange() != null) {

            String raw = dto.getFilter().getDatePublished().getRange().getGt();
            if (raw != null) {
                Map<String, Object> datePublishedMap = new HashMap<>();
                Map<String, Object> rangeMap = new HashMap<>();

                String formatted;
                if (raw.contains("-")) {
                    LocalDate date = LocalDate.parse(raw);
                    formatted = date.format(DateTimeFormatter.BASIC_ISO_DATE); // "20000101"
                } else {
                    formatted = raw;
                }

                rangeMap.put("gt", String.valueOf(formatted)); // Гарантируем String для Jackson
                datePublishedMap.put("range", rangeMap);
                filterMap.put("date_published", datePublishedMap);
            }
        }

        if (!filterMap.isEmpty()) {
            body.put("filter", filterMap);
        }

        return body;
    }


}
