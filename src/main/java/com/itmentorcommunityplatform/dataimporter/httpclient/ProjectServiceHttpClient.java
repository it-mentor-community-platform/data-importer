package com.itmentorcommunityplatform.dataimporter.httpclient;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.ProjectUpsertRequestDto;
import com.itmentorcommunityplatform.dataimporter.dto.response.ProjectReviewSheetsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceHttpClient {

    private final DataImporterProperties props;
    private final WebClient webClient;

    public void upsertProject(ProjectUpsertRequestDto requestDto) {

        String uri = props.getProjectServiceBaseUrl() + "/api/project/internal/project";

        try {
            webClient.post()
                    .uri(uri)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestDto)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(10));

            log.info("Successfully upsert project in ProjectService: authorTelegramUserId={}, roadmapProject={}",
                    requestDto.authorTelegramUserId(), requestDto.roadmapProject());
        } catch (WebClientResponseException wcre) {
            log.error("ProjectService returned error. status={}, body={}, telegramId={}, roadmapProject={}",
                    wcre.getStatusCode().value(), wcre.getResponseBodyAsString(), requestDto.authorTelegramUserId(), requestDto.roadmapProject());
            throw wcre;
        } catch (Exception ex) {
            log.error("Failed upsert project in ProjectService: authorTelegramUserId={}, roadmapProject={}",
                    requestDto.authorTelegramUserId(), requestDto.roadmapProject());
        }
    }

    public void upsertProjectReview(ProjectReviewSheetsDto requestDto) {

        String uri = props.getProjectServiceBaseUrl() + "/api/project/internal/review";

        try {
            webClient.post()
                    .uri(uri)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestDto)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(10));

            log.info("Successfully upsert project review in ProjectService: reviewerTelegramId={}, githubRepositoryUrl={}",
                    requestDto.getReviewerTelegramUserId(), requestDto.getReviewUrl());
        } catch (WebClientResponseException wcre) {
            log.error("ProjectService returned error. status={}, body={}, reviewerTelegramId={}, githubRepositoryUrl={}",
                    wcre.getStatusCode().value(), wcre.getResponseBodyAsString(),
                    requestDto.getReviewerTelegramUserId(), requestDto.getReviewUrl());
            throw wcre;
        } catch (Exception ex) {
            log.error("Failed upsert project review in ProjectService: reviewerTelegramId={}, githubRepositoryUrl={}",
                    requestDto.getReviewerTelegramUserId(), requestDto.getReviewUrl());
        }
    }
}
