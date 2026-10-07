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

            String firstName = "-";
            String telegramUri = "-";
            String shortTgName = "-";

            if (telegramProfileUrl != null && !telegramProfileUrl.isBlank()) {
                ProfileByTelegramDataResponse response
                        = profileServiceHttpClient.getProfileByTelegramUrl(telegramProfileUrl);

                validateProfileUrl(telegramProfileUrl, response);

                firstName = getFirstNameOrDefault(response);
                telegramUri = response.details().telegramUrl();
                shortTgName = extractTelegramUsername(telegramUri);
            }

            ValueRange valueRange = new ValueRange()
                    .setValues(
                            buildReviewSheetRow(
                                    reviewCreatedEvent,
                                    REVIEW_TYPE,
                                    firstName,
                                    shortTgName,
                                    telegramUri
                            )
                    );

            googleSheetsClient.addReviewToSheets(valueRange);
        } catch (IllegalArgumentException illegalArgumentException) {
            log.error(
                    "Invalid review author data: reviewUrl={}, reason={}",
                    reviewCreatedEvent.url(),
                    illegalArgumentException.getMessage(),
                    illegalArgumentException
            );

            throw illegalArgumentException;
        }
    }

    private void validateProfileUrl(String telegramUrl, ProfileByTelegramDataResponse response) {
        if (response == null
                || response.details() == null
                || response.details().telegramUrl() == null
                || response.details().telegramUrl().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Profile data not found for Telegram = " + telegramUrl
            );
        }
    }

    private String getFirstNameOrDefault(ProfileByTelegramDataResponse response) {
        String firstName = response.details().firstName();
        return firstName == null || firstName.isBlank() ? "-" : firstName;
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
