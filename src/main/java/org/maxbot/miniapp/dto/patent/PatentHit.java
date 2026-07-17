package org.maxbot.miniapp.dto.patent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatentHit {

    private Common common = new Common();       // общие данные (номер, дата, классификация)
    private Meta meta = new Meta();             // источник данных
    private Biblio biblio = new Biblio();       // библиография на разных языках
    private List<Drawing> drawings = Collections.emptyList();    // чертежи
    private String id = "";
    private String index = "";          // индекс набора данных
    private String dataset = "";        // набор данных (cis, ru_since_1994)
    private double similarity = 0.0;    // оценка похожести
    @JsonProperty("similarity_norm")
    private double similarityNorm = 0.0;            // нормированная похожесть
    private Snippet snippet = new Snippet();        // краткое описание

    // --- Библиография на разных языках ---
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Biblio {
        private BiblioLang ru = new BiblioLang();
        private BiblioLang en = new BiblioLang();
    }

    // --- Библиография на одном языке ---
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BiblioLang {
        private String title = "";                                       // название изобретения
        private List<NameWrapper> inventor = Collections.emptyList();    // изобретатели
        private List<NameWrapper> patentee = Collections.emptyList();    // патентообладатели
        private List<NameWrapper> applicant = Collections.emptyList();   // заявители

        private String citations = "";   // строка цитирований

        @JsonProperty("citations_parsed")
        private List<CitationParsed> citationsParsed = Collections.emptyList();  // структурированные цитаты
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NameWrapper {
        private String name = "";
    }

    // --- Цитаты/документы ---
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CitationParsed {
        private String text = "";        // текст цитаты
        private CitationDoc doc = new CitationDoc();    // структурированные данные документа
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CitationDoc {
        @JsonProperty("document_number")
        private String documentNumber = "";
        private String kind = "";
        private String identity = "";
        @JsonProperty("publication_date")
        private String publicationDate = "";
        private String id = "";
        @JsonProperty("publishing_office")
        private String publishingOffice = "";
    }

    // --- Чертежи патента ---
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Drawing {
        private String url = "";
        private String width = "";
        private String height = "";
    }

    // --- Общая информация о патенте ---
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Common {
        @JsonProperty("publishing_office")
        private String publishingOffice = "";                           // офис публикации (RU, UA, EA)
        @JsonProperty("document_number")
        private String documentNumber = "";                             // номер документа
        private String kind = "";                                       // тип документа (C2, B1)
        @JsonProperty("publication_date")
        private String publicationDate = "";                            // дата публикации

        private List<Priority> priority = Collections.emptyList();       // приоритеты
        private Application application = new Application();             // данные заявки

        private Classification classification = new Classification();    // классификация IPC/CPC
    }

    // --- Приоритеты патента ---
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Priority {
        private String number = "";         // номер приоритета
        private String country = "";        // страна
        @JsonProperty("filing_date")
        private String filingDate = "";     // дата подачи
    }

    // --- Данные заявки ---
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Application {
        private String number = "";                 // номер заявки
        @JsonProperty("filing_date")
        private String filingDate = "";             // дата подачи
        @JsonProperty("rights_start_date")
        private String rightsStartDate = "";        // дата начала действия прав
    }

    // --- Классификация патента (IPC/CPC) ---
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Classification {
        private List<IpcItem> ipc = Collections.emptyList();     // список IPC-классов
    }

    // --- Один класс IPC ---
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IpcItem {
        @JsonProperty("main_group")
        private String mainGroup = "";
        @JsonProperty("classification_value")
        private String classificationValue = "";
        private String subgroup = "";
        private String subclass = "";
        private String section = "";
        private String fullname = "";
        @JsonProperty("class")
        private String clazz = "";
    }

    // --- Метаданные ---
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        private Source source = new Source();
    }

    // --- Источник ---
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Source {
        private String path = "";    // путь к XML
        private String file = "";    // имя файла
        private String index = "";   // индекс набора
        private String from = "";    // источник
    }

    // --- Краткое описание патента ---
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Snippet {
        private String title = "";
        private String description = "";
        private String lang = "";
        private String applicant = "";
        private String inventor = "";
        private String patentee = "";

        private SnippetClassification classification;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SnippetClassification {
        private String ipc = "";
    }

}

