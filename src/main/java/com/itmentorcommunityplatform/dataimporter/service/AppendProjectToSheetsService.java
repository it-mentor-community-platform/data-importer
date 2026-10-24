package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.event.ProjectCreatedEvent;
import com.itmentorcommunityplatform.dataimporter.dto.response.ProjectSheetsDto;
import com.itmentorcommunityplatform.dataimporter.google.GoogleSheetsClient;
import com.itmentorcommunityplatform.dataimporter.mapper.ProjectSheetsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor

public class AppendProjectToSheetsService {

    private final ProjectSheetsMapper mapper;
    private final GoogleSheetsClient googleSheetsClient;
    private final DataImporterProperties props;

    public void addProjectToSheets(ProjectCreatedEvent projectCreatedEvent) {
        googleSheetsClient.addRowsToSheets(
                props.getProjectSpreadsheetId(),
                props.getSheetRangeProjects(),
                buildProjectSheetRow(projectCreatedEvent)
        );
    }

    private List<List<Object>> buildProjectSheetRow(ProjectCreatedEvent projectCreatedEvent) {

        ProjectSheetsDto projectSheetsDto =
                mapper.mapToSheetRow(projectCreatedEvent);

        return List.of(List.of(
                projectSheetsDto.getDate(),
                projectSheetsDto.getRoadmapProject().toLowerCase(),
                projectSheetsDto.getProgrammingLanguage(),
                projectSheetsDto.getRepositoryName(),
                projectSheetsDto.getGithubRepositoryUrl(),
                projectSheetsDto.getGithubUsername(),
                projectSheetsDto.getGithubUserUrl(),
                props.getHasReviewFormula(),
                props.getHasSubscriptionFormula()
        ));
    }
}
