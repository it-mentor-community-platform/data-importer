package com.itmentorcommunityplatform.dataimporter.service;

import com.google.api.services.sheets.v4.model.ValueRange;
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

    public void addProjectToSheets(ProjectCreatedEvent projectCreatedEvent) {

        ValueRange valueRange = new ValueRange()
                .setValues(buildProjectSheetRow(projectCreatedEvent));

        googleSheetsClient.addProjectToSheets(valueRange);

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
                "=ЕСЛИ(СЧЁТЕСЛИ(Reviews!D:D; INDIRECT(\"E\"&ROW())) + " +
                        "СЧЁТЕСЛИ('Спонсируемые ревью'!B:B; INDIRECT(\"E\"&ROW())) > 0; \"Есть\"; \"Нет\")"
        ));
    }
}
