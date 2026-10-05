package com.itmentorcommunityplatform.dataimporter.mapper;


import com.itmentorcommunityplatform.dataimporter.dto.event.ReviewCreatedEvent;
import com.itmentorcommunityplatform.dataimporter.dto.response.ReviewSheets;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Mapper(componentModel = "spring")
public interface ReviewSheetsMapper {

    @Mapping(
            target = "period",
            expression = "java(toMonthYear(reviewCreatedEvent.addedTimestamp()))"
    )
    @Mapping(
            target = "roadmapProject",
            expression = "java(extractRoadmapProject(reviewCreatedEvent.project().roadmapProject()))"
    )
    @Mapping(
            target = "programmingLanguage",
            source = "reviewCreatedEvent.project.programmingLanguage"
    )
    @Mapping(
            target = "githubRepositoryUrl",
            source = "reviewCreatedEvent.project.githubRepositoryUrl"
    )
    @Mapping(
            target = "reviewType",
            source = "reviewType"
    )
    @Mapping(
            target = "reviewUrl",
            source = "reviewCreatedEvent.url"
    )
    @Mapping(
            target = "reviewAuthorName",
            source = "reviewAuthorName"
    )
    @Mapping(
            target = "reviewAuthorTelegramName",
            source = "reviewerShortTgName"
    )
    @Mapping(
            target = "reviewAuthorTelegramUrl",
            source = "reviewerFullTgName"
    )
    ReviewSheets mapToReviewSheets(
            ReviewCreatedEvent reviewCreatedEvent,
            String reviewType,
            String reviewAuthorName,
            String reviewerShortTgName,
            String reviewerFullTgName
    );

    @Named("toMonthYear")
    default String toMonthYear(Long timestamp) {

        Instant instant = Instant.ofEpochSecond(timestamp);

        ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());

        String formatted = zdt.format(
                DateTimeFormatter.ofPattern("LLLL, yyyy", Locale.forLanguageTag("ru"))
        );

        return formatted.substring(0, 1).toUpperCase() + formatted.substring(1);

    }

    @Named("extractRoadmapProject")
    default String extractRoadmapProject(String roadmapProject) {
        if (roadmapProject == null || roadmapProject.isBlank()) {
            return null;
        }

        return roadmapProject.replace('_', '-');
    }
}
