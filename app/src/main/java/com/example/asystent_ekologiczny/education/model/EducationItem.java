package com.example.asystent_ekologiczny.education.model;

public class EducationItem {
    private final String title;
    private final String description;
    private final String videoUrl;
    private final String thumbnailUrl; // Dodane pole

    // Zaktualizowany konstruktor
    public EducationItem(String title, String description, String videoUrl, String thumbnailUrl) {
        this.title = title;
        this.description = description;
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
    }

    // Stary konstruktor (dla kompatybilności, jeśli gdzieś używasz)
    public EducationItem(String title, String description, String videoUrl) {
        this(title, description, videoUrl, null);
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getVideoUrl() { return videoUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; } // Getter
}
