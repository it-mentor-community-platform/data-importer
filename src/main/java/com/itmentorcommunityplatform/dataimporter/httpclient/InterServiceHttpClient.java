package com.itmentorcommunityplatform.dataimporter.httpclient;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.ProfileUpsertRequestDto;
import com.itmentorcommunityplatform.dataimporter.dto.request.UserUpsertRequestDto;
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
public class InterServiceHttpClient {

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
}