package com.itmentorcommunityplatform.dataimporter.service.listener;

import com.itmentorcommunityplatform.dataimporter.dto.event.ProjectCreatedEvent;
import com.itmentorcommunityplatform.dataimporter.service.AppendProjectToSheetsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectEventsListener {

    private static final String DATA_IMPORTER_SOURCE = "DATA_IMPORTER";
    private final AppendProjectToSheetsService appendProjectToSheetsService;


    @KafkaListener(
            topics = "${spring.kafka.topic.projects-project-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenProjectCreated(ProjectCreatedEvent event) {
        if (DATA_IMPORTER_SOURCE.equalsIgnoreCase(event.projectSourceType())) {
            log.debug("Skipping project event from source: {}", event.projectSourceType());
            return;
        }

        appendProjectToSheetsService.addProjectToSheets(event);
        log.info("Received new project event: {}", event);
    }
}