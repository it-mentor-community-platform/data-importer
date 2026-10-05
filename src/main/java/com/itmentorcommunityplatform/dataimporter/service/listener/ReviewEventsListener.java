package com.itmentorcommunityplatform.dataimporter.service.listener;

import com.itmentorcommunityplatform.dataimporter.dto.event.ProjectCreatedEvent;
import com.itmentorcommunityplatform.dataimporter.dto.event.ReviewCreatedEvent;
import com.itmentorcommunityplatform.dataimporter.service.AppendProjectToSheetsService;
import com.itmentorcommunityplatform.dataimporter.service.AppendReviewToSheetsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
    @RequiredArgsConstructor
    @Slf4j
    public class ReviewEventsListener {

        private static final String DATA_IMPORTER_SOURCE = "DATA_IMPORTER";
        private final AppendReviewToSheetsService appendReviewToSheetsService;

        @KafkaListener(
                topics = "${spring.kafka.topic.reviews-review-created}",
                groupId = "${spring.kafka.consumer.group-id}",
                containerFactory = "kafkaListenerContainerFactory"
        )
        public void listenReviewCreated(ReviewCreatedEvent event) {
            if (DATA_IMPORTER_SOURCE.equalsIgnoreCase(event.reviewSourceType())) {
                log.debug("Skipping review event from source: {}", event.reviewSourceType());
                return;
            }

            appendReviewToSheetsService.addReviewToSheets(event);
            log.info("Received new review event: {}", event);
        }
}