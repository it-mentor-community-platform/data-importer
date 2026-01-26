package com.itmentorcommunityplatform.dataimporter.dto.event;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProjectCreatedEvent {

    private Long authorTelegramUserId;
    private String authorTelegramProfileUrl;
    private String githubRepositoryUrl;
    private String programmingLanguage;
    private String roadmapProject;
    private Long addedTimestamp;
    private String projectSourceType;

}