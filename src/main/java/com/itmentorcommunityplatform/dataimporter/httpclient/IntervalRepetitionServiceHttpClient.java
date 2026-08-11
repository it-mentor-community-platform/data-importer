package com.itmentorcommunityplatform.dataimporter.httpclient;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.QuestionUpsertRequestDto;
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
public class IntervalRepetitionServiceHttpClient {

    private final DataImporterProperties props;
    private final WebClient webClient;

    public void upsertQuestion(QuestionUpsertRequestDto requestDto) {
        String uri = props.getIntervalRepetitionServiceBaseUrl() + "/api/interval-repetition/internal/question";

        try {
            webClient.post()
                    .uri(uri)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestDto)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(10));

            log.info(
                    "Successfully upsert question in IntervalRepetitionService: specialization={}, category={}, title={}",
                    requestDto.specialization(),
                    requestDto.category(),
                    requestDto.title()
            );

        } catch (WebClientResponseException wcre) {
            log.error(
                    "IntervalRepetitionService returned error. status={}, body={}, specialization={}, category={}, title={}",
                    wcre.getStatusCode().value(),
                    wcre.getResponseBodyAsString(),
                    requestDto.specialization(),
                    requestDto.category(),
                    requestDto.title()
            );

            throw wcre;

        } catch (Exception ex) {
            log.error(
                    "Failed upsert question in IntervalRepetitionService: specialization={}, category={}, title={}",
                    requestDto.specialization(),
                    requestDto.category(),
                    requestDto.title(),
                    ex
            );
        }

    }

}
