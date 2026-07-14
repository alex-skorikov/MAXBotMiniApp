package org.maxbot.miniapp.service;

import org.maxbot.miniapp.dto.patent.PatentHit;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class PatentCardService {
    public static String formatPatentCard(PatentHit card) {

        String title = card.getBiblio().getRu().getTitle();

        // МПК
        String ipc = card.getCommon().getClassification().getIpc().stream()
                .map(ip -> ip.getFullname())
                .collect(Collectors.joining(", "));

        // Документ
        String doc = String.format("%s %s %s (%s)",
                card.getCommon().getPublishingOffice(),
                card.getCommon().getDocumentNumber(),
                card.getCommon().getKind(),
                card.getCommon().getPublicationDate()
        );

        // Приоритет
        String priority = String.format("%s %s от %s",
                card.getCommon().getPriority().getCountry(),
                card.getCommon().getPriority().getNumber(),
                card.getCommon().getPriority().getFilingDate()
        );

        // Заявка
        String application = String.format("%s от %s",
                card.getCommon().getApplication().getNumber(),
                card.getCommon().getApplication().getFilingDate()
        );

        // Заявители
        String applicants = card.getBiblio().getRu().getApplicant().stream()
                .map(a -> "• " + a.getName())
                .collect(Collectors.joining("\n"));

        // Авторы
        String inventors = card.getBiblio().getRu().getInventor().stream()
                .map(a -> "• " + a.getName())
                .collect(Collectors.joining("\n"));

        // Описание
        String description = card.getSnippet().getDescription();

        return String.format(
                "📄 %s\n" +
                        "МПК: %s\n" +
                        "Документ: %s\n" +
                        "Приоритет: %s\n" +
                        "Заявка: %s\n\n" +
                        "Заявители:\n%s\n\n" +
                        "Авторы:\n%s\n\n" +
                        "Описание:\n%s\n",
                title, ipc, doc, priority, application, applicants, inventors, description
        );
    }

}
