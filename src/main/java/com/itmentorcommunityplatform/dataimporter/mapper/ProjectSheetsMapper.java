package com.itmentorcommunityplatform.dataimporter.mapper;


import com.itmentorcommunityplatform.dataimporter.dto.event.ProjectCreatedEvent;
import com.itmentorcommunityplatform.dataimporter.dto.response.ProjectSheetsDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Mapper(componentModel = "spring")
public interface ProjectSheetsMapper {

    @Mapping(target = "date", expression = "java(toMonthYear(projectCreatedEvent.getAddedTimestamp()))")
    @Mapping(target = "githubUserUrl", expression = "java(extractGithubUserUrl(projectCreatedEvent.getGithubRepositoryUrl()))")
    @Mapping(target = "githubUsername", expression = "java(extractGithubUsername(projectCreatedEvent.getGithubRepositoryUrl()))")
    @Mapping(target = "repositoryName", expression = "java(extractRepositoryName(projectCreatedEvent.getGithubRepositoryUrl()))")
    @Mapping(target = "roadmapProject", expression = "java(extractRoadmapProject(projectCreatedEvent.getRoadmapProject()))")
    ProjectSheetsDto mapToSheetRow(ProjectCreatedEvent projectCreatedEvent);


    @Named("extractRepositoryName")
    default String extractRepositoryName(String githubUrl) {
        if (githubUrl == null || githubUrl.isBlank()) {
            return null;
        }

        return githubUrl.substring(githubUrl.lastIndexOf("/") + 1);
    }

    @Named("extractRoadmapProject")
    default String extractRoadmapProject(String roadmapProject) {
        if (roadmapProject == null || roadmapProject.isBlank()) {
            return null;
        }

        return roadmapProject.replace('_','-');
    }

    @Named("extractGithubUsername")
    default String extractGithubUsername(String githubUrl) {

        if (githubUrl == null || githubUrl.isBlank()) {
            return null;
        }

        String idx = githubUrl
                .replace("https://github.com/", "");

        return idx.substring(0, idx.lastIndexOf("/"));
    }

    @Named("extractGithubUserUrl")
    default String extractGithubUserUrl(String githubUrl) {
        if (githubUrl == null || githubUrl.isBlank()) {
            return null;
        }
        return githubUrl.substring(0, githubUrl.lastIndexOf("/"));
    }

    @Named("toMonthYear")
    default String toMonthYear(Long timestamp) {

        Instant instant = Instant.ofEpochSecond(timestamp);

        ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());

        String formatted = zdt.format(
                DateTimeFormatter.ofPattern("LLLL, yyyy", Locale.forLanguageTag("ru"))
        );

        return formatted.substring(0, 1).toUpperCase() + formatted.substring(1);

    }

}
