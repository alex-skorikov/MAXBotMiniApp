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

    private Common common;
    private Meta meta;
    private Biblio biblio;
    private List<Drawing> drawings= Collections.emptyList();
    private String id;
    private String index;
    private String dataset;
    private double similarity;
    @JsonProperty("similarity_norm")
    private double similarityNorm;
    private Snippet snippet;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Biblio {
        private BiblioRu ru;
        private BiblioEn en;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BiblioRu {
        private String citations;
        private List<NameWrapper> inventor= Collections.emptyList();
        private String title;
        private List<NameWrapper> patentee= Collections.emptyList();
        private List<NameWrapper> applicant= Collections.emptyList();
        @JsonProperty("citations_parsed")
        private List<CitationParsed> citationsParsed= Collections.emptyList();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BiblioEn {
        private String citations;
        private List<NameWrapper> inventor= Collections.emptyList();
        private String title;
        private List<NameWrapper> patentee= Collections.emptyList();
        private List<NameWrapper> applicant= Collections.emptyList();
        @JsonProperty("citations_parsed")
        private List<CitationParsed> citationsParsed= Collections.emptyList();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NameWrapper {
        private String name;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CitationParsed {
        private String text;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Drawing {
        private String url;
        private String width;
        private String height;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Common {
        @JsonProperty("publishing_office")
        private String publishingOffice;
        @JsonProperty("document_number")
        private String documentNumber;
        private String kind;
        @JsonProperty("publication_date")
        private String publicationDate;

        private Priority priority = new Priority();
        private Application application;

        private Classification classification;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Priority {
        private String number ="";
        private String country="";
        @JsonProperty("filing_date")
        private String filingDate="";
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Application {
        private String number;
        @JsonProperty("filing_date")
        private String filingDate;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Classification {
        private List<IpcItem> ipc= Collections.emptyList();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IpcItem {
        @JsonProperty("main_group")
        private String mainGroup;
        @JsonProperty("classification_value")
        private String classificationValue;
        private String subgroup;
        private String subclass;
        private String section;
        private String fullname;
        @JsonProperty("class")
        private String clazz;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        private Source source;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Source {
        private String path;
        private String file;
        private String index;
        private String from;
    }
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Snippet {
        private String title;
        private String description;
        private String lang;
        private String applicant;
        private String inventor;
        private String patentee;

        private SnippetClassification classification;
    }
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SnippetClassification {
        private String ipc;
    }

}

