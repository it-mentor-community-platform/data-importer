package com.itmentorcommunityplatform.dataimporter.model;

public enum RoadmapProjectType {
    HANGMAN("Виселица"),
    SIMULATION("Симуляция"),
    CURRENCY_EXCHANGE("Обмен валют"),
    TENNIS_SCOREBOARD("Теннисное табло"),
    WEATHER_VIEWER("Погода"),
    CLOUD_FILE_STORAGE("Облачное хранилище файлов"),
    TASK_TRACKER("Планировщик задач"),
    OTHER("");

    private final String russianName;

    RoadmapProjectType(String russianName) {
        this.russianName = russianName;
    }

    public static RoadmapProjectType fromRussianName(String text) {
        for (RoadmapProjectType type : RoadmapProjectType.values()) {
            if (type.russianName.equalsIgnoreCase(text.trim())) {
                return type;
            }
        }
        return OTHER;
    }
}
