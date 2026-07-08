package org.maxbot.miniapp.dto.patent;

import java.util.List;
import java.util.stream.Collectors;

public class PatentSearchResponse {

    private List<PatentHit> hits;

//    public List<MiniAppCard> toCards() {
//        return hits.stream()
//                .map(hit -> new MiniAppCard(
//                        hit.getId(),
//                        hit.getBiblio().getRu().getTitle(),
//                        "Владелец: " + hit.getBiblio().getRu().getApplicant(),
//                        "Дата: " + hit.getBiblio().getRu().getDate()
//                ))
//                .collect(Collectors.toList());
//    }
}

