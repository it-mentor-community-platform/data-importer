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

    /**
     * addReviewToSheets as it is now (07.10.26) is a temporary decision that solves the problem
     * when telegramProfileUrl = null (since it's discovered it's nullable)
     * <p>
     * see for details:
     * <a href="https://github.com/it-mentor-community-platform/meta/blob/main/system-analytics/services/
     * project-service/
     * index.md#producer-%D0%B4%D0%BB%D1%8F-%D1%82%D0%BE%D0%BF%D0%B8%D0%BA%D0%B0-reviewsreviewcreated">...</a>
     *
     * @param reviewCreatedEvent with needed params
     */
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

                firstName = checkAndCorrectFirstName(response);
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

    /**
     * NOTE: this is primarily a fallback for local/test data when firstName is missing.
     *
     * @param response
     * @return original name if it's not empty/blank or "-" instead
     */

    private String checkAndCorrectFirstName(ProfileByTelegramDataResponse response) {
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
