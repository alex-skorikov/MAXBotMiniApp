package org.maxbot.miniapp.dto.patent;

import lombok.Data;

@Data
public class PatentHit {

    private String id; // RU123456
    private Biblio biblio;

    @Data
    public static class Biblio {
        private Ru ru;
    }

    @Data
    public static class Ru {
        private String title;       // Название патента
        private String applicant;   // Владелец
        private String date;        // Дата публикации
    }
}

