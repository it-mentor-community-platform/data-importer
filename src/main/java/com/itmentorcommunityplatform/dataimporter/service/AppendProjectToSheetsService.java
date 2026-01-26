package com.itmentorcommunityplatform.dataimporter.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.event.ProjectCreatedEvent;
import com.itmentorcommunityplatform.dataimporter.dto.response.ProjectSheetsDto;
import com.itmentorcommunityplatform.dataimporter.mapper.ProjectSheetsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor

public class AppendProjectToSheetsService {


    private final DataImporterProperties properties;
    private final ProjectSheetsMapper mapper;
    private final Sheets sheet;


    public void addProjectToSheets(ProjectCreatedEvent projectCreatedEvent) {

        ValueRange valueRange = new ValueRange()
                .setValues(buildProjectSheetRow(projectCreatedEvent));

        try {
            sheet.spreadsheets()
                    .values()
                    .append(properties.getSpreadsheetId(), properties.getSheetRangeProjects(), valueRange)
                    .setValueInputOption("USER_ENTERED")
                    .execute();

        } catch (Exception e) {
            log.error("[Sheets] Failed to append row spreadsheetId={}, range={}, authorTelegramUserId={}",
                    properties.getSpreadsheetId(),
                    properties.getSheetRangeProjects(),
                    projectCreatedEvent.getAuthorTelegramUserId(),
                    e);

            throw new RuntimeException("Failed to append to Google Sheets", e);
        }

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
                projectSheetsDto.getGithubUserUrl()
        ));
    }
}
