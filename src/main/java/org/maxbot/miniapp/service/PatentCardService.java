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
                .map(PatentHit.IpcItem::getFullname)
                .collect(Collectors.joining(", "));

        // Документ
        String doc = String.format("%s %s %s (%s)",
                card.getCommon().getPublishingOffice(),
                card.getCommon().getDocumentNumber(),
                card.getCommon().getKind(),
                card.getCommon().getPublicationDate()
        );

        // Заявители
        String applicants = card.getBiblio().getRu().getApplicant().stream()
                .map(a -> "• " + a.getName())
                .collect(Collectors.joining("\n"));

        // Авторы
        String inventors = card.getBiblio().getRu().getInventor().stream()
                .map(a -> "• " + a.getName())
                .collect(Collectors.joining("\n"));

        // Описание 300 символов
        String description = card.getSnippet().getDescription().length() > 300
                ? card.getSnippet().getDescription().substring(0, 300) + "…"
                : card.getSnippet().getDescription();

        return String.format(
                "📄 %s\n" +
                        "МПК: %s\n" +
                        "Документ: %s\n" +
                        "Заявители:\n%s\n\n" +
                        "Авторы:\n%s\n\n" +
                        "Описание:\n%s\n",
                title, ipc, doc, applicants, inventors, description
        );
    }

}
