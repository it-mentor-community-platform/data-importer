package com.itmentorcommunityplatform.dataimporter.httpclient;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.*;
import com.itmentorcommunityplatform.dataimporter.dto.response.ProfileByGithubResponseDto;
import com.itmentorcommunityplatform.dataimporter.dto.response.ProjectReviewSheetsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceHttpClient {

    private final DataImporterProperties props;
    private final WebClient webClient;

    public void upsertUser(UserUpsertRequestDto request) {
        String url = props.getAuthServiceBaseUrl() + "/api/auth/internal/user";
        try {
            webClient.post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(10));
            log.info("Successfully upserted user in Auth Service: telegramId={}, roles={}",
                    request.telegramUserId(), request.roles());
        } catch (WebClientResponseException wcre) {
            log.error("Auth Service returned error. status={}, body={}, telegramId={}",
                    wcre.getStatusCode().value(), wcre.getResponseBodyAsString(), request.telegramUserId());
            throw wcre;
        } catch (Exception ex) {
            log.error("Failed to call Auth Service for telegramId={}, error={}",
                    request.telegramUserId(), ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

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

    public void upsertProfile(ProfileUpsertRequestDto request) {
        String url = props.getProfileServiceBaseUrl() + "/api/profile/internal/profile";
        try {
            webClient.post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(10));
            log.info("Successfully upserted profile in Profile Service: telegramId={}, details={}",
                    request.telegramUserId(), request.details().toString().substring(10));
        } catch (WebClientResponseException wcre) {
            log.error("Profile Service returned error. status={}, body={}, telegramId={}",
                    wcre.getStatusCode().value(), wcre.getResponseBodyAsString(), request.telegramUserId());
            throw wcre;
        } catch (Exception ex) {
            log.error("Failed to call Profile Service for telegramId={}, error={}",
                    request.telegramUserId(), ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    public ProfileByGithubResponseDto getProfileByGithubUrl(String githubProfileUrl) {

        String url = UriComponentsBuilder
                .fromUri(URI.create(props.getProfileServiceBaseUrl()))
                .path("/api/profile/internal/profile/by-github-profile-url")
                .queryParam("url", githubProfileUrl)
                .toUriString();

        try {
            ProfileByGithubResponseDto response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(ProfileByGithubResponseDto.class)
                    .block(Duration.ofSeconds(10));

            log.info("Fetched OK for GitHub URL: {}", githubProfileUrl);
            return response;

        } catch (Exception ex) {
            log.error("Failed. url={}, error={}", githubProfileUrl, ex.getMessage());
            return null;
        }
    }

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

    public Long getTelegramUserIdByUrl(String telegramUrl) {
        String url = UriComponentsBuilder
                .fromUri(URI.create(props.getProfileServiceBaseUrl()))
                .path("/api/profile/internal/profile/by-telegram-url")
                .queryParam("url", telegramUrl)
                .toUriString();

        try {
            ProfileByGithubResponseDto response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(ProfileByGithubResponseDto.class)
                    .block(Duration.ofSeconds(5));

            return response != null ? response.telegramUserId() : null;

        } catch (Exception ex) {
            log.error("Failed to find profile for telegramUrl={}, error={}", telegramUrl, ex.getMessage());
            return null;
        }
    }
}