package com.example.asystent_ekologiczny.education.model;

public class EducationItem {
    private final String title;
    private final String description;
    private final String videoUrl;

    public EducationItem(String title, String description, String videoUrl) {
        this.title = title;
        this.description = description;
        this.videoUrl = videoUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getVideoUrl() {
        return videoUrl;
    }
}

