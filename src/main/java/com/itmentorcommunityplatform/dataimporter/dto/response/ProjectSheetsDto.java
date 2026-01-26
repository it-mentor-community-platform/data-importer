package com.itmentorcommunityplatform.dataimporter.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectSheetsDto {

    private String date;
    private String roadmapProject;
    private String programmingLanguage;
    private String repositoryName;
    private String githubRepositoryUrl;
    private String githubUsername;
    private String githubUserUrl;

}
