package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
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
    private final DataImporterProperties props;

    private static final String REVIEW_TYPE = "Заметки";

    public void addReviewToSheets(ReviewCreatedEvent reviewCreatedEvent) {
        String telegramProfileUrl = reviewCreatedEvent.reviewerTelegramProfileUrl();

        ReviewerData reviewerData = resolveReviewerData(telegramProfileUrl);

        googleSheetsClient.addRowsToSheets(
                props.getProjectSpreadsheetId(),
                props.getSheetRangeProjectReviews(),
                buildReviewSheetRow(
                        reviewCreatedEvent,
                        REVIEW_TYPE,
                        reviewerData.firstName(),
                        reviewerData.shortTgName(),
                        reviewerData.telegramUri()
                )
        );
    }

    private record ReviewerData(
            String firstName,
            String telegramUri,
            String shortTgName
    ) {
    }

    private ReviewerData resolveReviewerData(String telegramProfileUrl) {
        String firstName = "-";
        String telegramUri = "-";
        String shortTgName = "-";

        if (telegramProfileUrl == null || telegramProfileUrl.isBlank()) {
            return new ReviewerData(firstName, telegramUri, shortTgName);
        }

        ProfileByTelegramDataResponse response
                = profileServiceHttpClient.getProfileByTelegramUrl(telegramProfileUrl);
        if (response == null || response.details() == null) {
            return new ReviewerData(firstName, telegramUri, shortTgName);
        }

        firstName = getFirstNameOrDefault(response);
        String profileTelegramUrl = response.details().telegramUrl();

        if (profileTelegramUrl != null && !profileTelegramUrl.isBlank()) {
            telegramUri = profileTelegramUrl;
            shortTgName = extractTelegramUsername(telegramUri);
        }
        return new ReviewerData(firstName, telegramUri, shortTgName);
    }

    private String getFirstNameOrDefault(ProfileByTelegramDataResponse response) {
        String firstName = response.details().firstName();
        return firstName == null || firstName.isBlank() ? "-" : firstName;
    }

    private List<List<String>> buildReviewSheetRow(
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
