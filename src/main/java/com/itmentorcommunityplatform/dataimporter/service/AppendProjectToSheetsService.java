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

    private static final String HAS_REVIEW_FORMULA = """
            =ЕСЛИ
            (СЧЁТЕСЛИ(Reviews!D:D; INDIRECT("E"&ROW()))+
            СЧЁТЕСЛИ('Спонсируемые ревью'!B:B; INDIRECT("E"&ROW())) > 0; "Есть"; "Нет")
            """;

    public void addProjectToSheets(ProjectCreatedEvent projectCreatedEvent) {
        googleSheetsClient.addRowsToSheets(
                props.getProjectSpreadsheetId(),
                props.getSheetRangeProjects(),
                buildProjectSheetRow(projectCreatedEvent)
        );
    }

    private List<List<String>> buildProjectSheetRow(ProjectCreatedEvent projectCreatedEvent) {

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
                HAS_REVIEW_FORMULA
        ));
    }
}
