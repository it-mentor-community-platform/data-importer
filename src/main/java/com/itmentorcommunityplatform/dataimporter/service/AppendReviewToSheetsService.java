package com.itmentorcommunityplatform.dataimporter.service;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.itmentorcommunityplatform.dataimporter.dto.event.ReviewCreatedEvent;
import com.itmentorcommunityplatform.dataimporter.dto.response.ProfileByTelegramDataResponse;
import com.itmentorcommunityplatform.dataimporter.dto.response.ReviewSheets;
import com.itmentorcommunityplatform.dataimporter.google.GoogleSheetsClient;
import com.itmentorcommunityplatform.dataimporter.httpclient.ProfileServiceHttpClient;
import com.itmentorcommunityplatform.dataimporter.mapper.ReviewSheetsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.itmentorcommunityplatform.dataimporter.util.TelegramLinkUtil.extractTelegramUsername;

@Slf4j
@Service
@RequiredArgsConstructor

public class AppendReviewToSheetsService {

    private final ReviewSheetsMapper mapper;
    private final GoogleSheetsClient googleSheetsClient;
    private final ProfileServiceHttpClient profileServiceHttpClient;

    private static final String REVIEW_TYPE = "Заметки";

    public void addReviewToSheets(ReviewCreatedEvent reviewCreatedEvent) {
        try {
            String telegramProfileUrl = reviewCreatedEvent.reviewerTelegramProfileUrl();

            ProfileByTelegramDataResponse response
                    = profileServiceHttpClient.getProfileByTelegramUrl(telegramProfileUrl);

            validateProfileData(telegramProfileUrl, response);

            String telegramUri = response.details().telegramUrl();
            String shortTgName = extractTelegramUsername(telegramUri);

            ValueRange valueRange = new ValueRange()
                    .setValues(
                            buildReviewSheetRow(
                                    reviewCreatedEvent,
                                    REVIEW_TYPE,
                                    response.details().firstName(),
                                    shortTgName,
                                    telegramUri
                            )
                    );

            //toDo разобраться с гуглТаблицами
            googleSheetsClient.addReviewToSheets(valueRange);
        } catch (IllegalArgumentException illegalArgumentException) {
            log.error(
                    "Failed to append review to Google Sheets: reviewUrl={}, reason={}",
                    reviewCreatedEvent.url(),
                    illegalArgumentException.getMessage(),
                    illegalArgumentException
            );

            throw illegalArgumentException;
        }
    }

    private void validateProfileData(String telegramUrl, ProfileByTelegramDataResponse response) {
        if (response == null
                || response.details() == null
                || response.details().firstName() == null
                || response.details().firstName().isBlank()
                || response.details().telegramUrl() == null
                || response.details().telegramUrl().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Profile data not found for Telegram = " + telegramUrl
            );
        }
    }

    private List<List<Object>> buildReviewSheetRow(
            ReviewCreatedEvent reviewCreatedEvent,
            String reviewType,
            String authorName,
            String shortTgName,
            String uri
    ) {

        ReviewSheets reviewSheets = mapper.mapToReviewSheets(
                reviewCreatedEvent,
                reviewType,
                authorName,
                shortTgName,
                uri
        );

        return List.of(List.of(
                reviewSheets.period(),
                reviewSheets.roadmapProject().toLowerCase(),
                reviewSheets.programmingLanguage(),
                reviewSheets.githubRepositoryUrl(),
                reviewSheets.reviewType(),
                reviewSheets.reviewUrl(),
                reviewSheets.reviewAuthorName(),
                reviewSheets.reviewAuthorTelegramName(),
                reviewSheets.reviewAuthorTelegramUrl()
        ));
    }
}
