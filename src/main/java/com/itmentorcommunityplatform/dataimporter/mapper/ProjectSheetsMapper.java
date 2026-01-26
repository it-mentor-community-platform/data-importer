package com.itmentorcommunityplatform.dataimporter.mapper;


import com.itmentorcommunityplatform.dataimporter.dto.event.ProjectCreatedEvent;
import com.itmentorcommunityplatform.dataimporter.dto.response.ProjectSheetsDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ProjectSheetsMapper {

    @Mapping(target = "date", expression = "java(toMonthYear(projectCreatedEvent.getAddedTimestamp()))")
    @Mapping(target = "githubUserUrl", expression = "java(extractGithubUserUrl(projectCreatedEvent.getGithubRepositoryUrl()))")
    @Mapping(target = "githubUsername", expression = "java(extractGithubUsername(projectCreatedEvent.getGithubRepositoryUrl()))")
    @Mapping(target = "repositoryName", expression = "java(extractRepositoryName(projectCreatedEvent.getGithubRepositoryUrl()))")
    ProjectSheetsDto mapToSheetRow(ProjectCreatedEvent projectCreatedEvent);


    @Named("extractRepositoryName")
    default String extractRepositoryName(String githubUrl) {
        if (githubUrl == null || githubUrl.isBlank()) {
            return null;
        }

        return githubUrl.substring(githubUrl.lastIndexOf("/") + 1);
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
        return java.time.Instant.ofEpochSecond(timestamp)
                .atZone(java.time.ZoneId.of("Europe/Berlin"))
                .format(java.time.format.DateTimeFormatter.ofPattern(
                        "LLLL, yyyy", java.util.Locale.forLanguageTag("ru")));
    }

}
