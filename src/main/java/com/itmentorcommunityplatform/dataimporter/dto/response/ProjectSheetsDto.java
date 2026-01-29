package com.itmentorcommunityplatform.dataimporter.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectSheetsDto {

    private String date;

    @JsonProperty("roadmap_project")
    private String roadmapProject;

    @JsonProperty("programming_language")
    private String programmingLanguage;

    private String repositoryName;

    @JsonProperty("github_repository_url")
    private String githubRepositoryUrl;

    private String githubUsername;

    private String githubUserUrl;

}
