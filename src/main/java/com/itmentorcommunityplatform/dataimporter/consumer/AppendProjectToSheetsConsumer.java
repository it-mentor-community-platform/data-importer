package com.itmentorcommunityplatform.dataimporter.consumer;

import com.itmentorcommunityplatform.dataimporter.dto.event.ProjectCreatedEvent;
import com.itmentorcommunityplatform.dataimporter.service.AppendProjectToSheetsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class AppendProjectToSheetsConsumer {

    private final AppendProjectToSheetsService appendProjectToSheetsService;

    @KafkaListener(topics = "projects.project.created", groupId = "data-importer-cg")
    public void consumerAppendProjectToSheetEvent(ProjectCreatedEvent event) {

        if (event == null) {
            log.warn("[Kafka Consumer] event is null]");
            return;
        }

        log.info("[Kafka Consumer] import project | author_telegram_user_id: {}," +
                        "author_telegram_profile_url: {}," +
                        " github_repository_url: {}," +
                        "programming_language: {}," +
                        "roadmap_project: {}," +
                        "added_timestamp: {}," +
                        "projeсt_source_type: {}", event.getAuthorTelegramUserId(),
                event.getAuthorTelegramProfileUrl(), event.getGithubRepositoryUrl(),
                event.getProgrammingLanguage(), event.getRoadmapProject(),
                event.getAddedTimestamp(), event.getProjectSourceType());

        try {

            if ("DATA_IMPORTER".equals(event.getProjectSourceType())) {
                log.debug("[Kafka Consumer] source DATA_IMPORTER event authorTelegramUserId={}", event.getAuthorTelegramUserId());
                return;
            }

            appendProjectToSheetsService.addProjectToSheets(event);

            log.info("[Kafka Consumer]  Project appended to Sheets authorTelegramUserId= {}", event.getAuthorTelegramUserId());
        } catch (Exception e) {
            log.error("[Kafka Consumer] Error processing event for user: {}. Error: {}",
                    event.getAuthorTelegramUserId(), e.getMessage(), e);
            throw e;
        }
    }
}
