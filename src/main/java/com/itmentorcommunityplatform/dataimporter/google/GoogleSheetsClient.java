package com.itmentorcommunityplatform.dataimporter.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleSheetsClient {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String SCOPE_SHEETS_READONLY = "https://www.googleapis.com/auth/spreadsheets.readonly";

    private final DataImporterProperties props;

    private Sheets sheetsService;

    @PostConstruct
    public void init() {
        try {
            GoogleCredentials credentials = loadCredentials();
            credentials = credentials.createScoped(Collections.singletonList(SCOPE_SHEETS_READONLY));
            sheetsService = new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JSON_FACTORY,
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("data-importer")
                    .build();
            log.info("Google Sheets client initialized successfully");
        } catch (IOException | GeneralSecurityException e) {
            log.error("Failed to initialize Google Sheets client: {}", e.getMessage(), e);
            throw new IllegalStateException("Unable to initialize Google Sheets client", e);
        }
    }

    public List<List<Object>> readSheet() throws IOException {
        String spreadsheetId = props.getSpreadsheetId();
        String range = props.getSheetRange();
        log.debug("Reading Google Sheet: id={}, range={}", spreadsheetId, range);
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        List<List<Object>> values = response.getValues();
        return values == null ? Collections.emptyList() : values;
    }

    private GoogleCredentials loadCredentials() throws IOException {
        String credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (credentialsPath != null && !credentialsPath.isBlank()) {
            log.info("Loading Google credentials from file: {}", credentialsPath);
            try (FileInputStream in = new FileInputStream(credentialsPath)) {
                return GoogleCredentials.fromStream(in);
            }
        }

        String credentialsJson = System.getenv("GOOGLE_APPLICATION_CREDENTIALS_JSON");
        if (credentialsJson != null && !credentialsJson.isBlank()) {
            log.info("Loading Google credentials from environment variable");
            return GoogleCredentials.fromStream(
                    new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8))
            );
        }
        throw new IllegalStateException(
                "Neither GOOGLE_APPLICATION_CREDENTIALS nor GOOGLE_APPLICATION_CREDENTIALS_JSON is set");
    }
}
