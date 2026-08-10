package com.itmentorcommunityplatform.dataimporter.httpclient;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.GuaranteedReviewRequestDto;
import com.itmentorcommunityplatform.dataimporter.dto.request.MentorUpsertRequestDto;
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
public class MentorServiceHttpClient {

    private final DataImporterProperties props;
    private final WebClient webClient;

    public void insertMentor(MentorUpsertRequestDto request) {
        String url = props.getMentorServiceBaseUrl() + "/api/mentor/internal/mentor";
        try {
            webClient.post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(10));
            log.info("Successfully inserted mentor in Mentor Service: telegramId={}, telegramUrl={}",
                    request.mentorTelegramUserId(), request.telegramUrl());
        } catch (WebClientResponseException wcre) {
            if (wcre.getStatusCode().value() == 409) {
                log.info("Mentor with telegramId={} already exist in Mentor Service",
                        request.mentorTelegramUserId());
                return;
            }
            log.error("Mentor Service returned error. status={}, body={}, telegramId={}",
                    wcre.getStatusCode().value(), wcre.getResponseBodyAsString(), request.mentorTelegramUserId());
            throw wcre;
        } catch (Exception ex) {
            log.error("Failed to call Mentor Service for telegramId={}, error={}",
                    request.mentorTelegramUserId(), ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    public void upsertGuaranteedReview(GuaranteedReviewRequestDto requestDto, Long telegramUserId) {
        String uri = props.getMentorServiceBaseUrl() + "/api/mentor/internal/guaranteed-review";

        try {
            webClient.post()
                    .uri(uri)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Telegram-User-Id", String.valueOf(telegramUserId))
                    .bodyValue(requestDto)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(10));

            log.info("Successfully upserted guaranteed review: mentorUrl={}, language={}, projectType={}",
                    requestDto.getTelegramUrl(), requestDto.getLanguage(), requestDto.getProjectType());

        } catch (WebClientResponseException webClientEx) {
            log.error("Mentor Service returned error. status={}, body={}, mentorUrl={}",
                    webClientEx.getStatusCode().value(), webClientEx.getResponseBodyAsString(), requestDto.getTelegramUrl());
            throw webClientEx;
        } catch (Exception ex) {
            log.error("Failed to call Mentor Service for mentorUrl={}, error={}",
                    requestDto.getTelegramUrl(), ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}