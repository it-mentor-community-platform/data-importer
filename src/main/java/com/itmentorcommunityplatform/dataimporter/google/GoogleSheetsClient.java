package com.itmentorcommunityplatform.dataimporter.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleSheetsClient {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final DataImporterProperties props;

    @Value("${google.credentials.json:}")
    private String credentialsJson;

    private Sheets sheetsService;

    @PostConstruct
    public void init() {
        try {
            GoogleCredentials credentials = loadCredentials();
            credentials = credentials.createScoped(
                    Collections.singletonList(SheetsScopes.SPREADSHEETS)
            );
            sheetsService = new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JSON_FACTORY,
                    new HttpCredentialsAdapter(credentials)
            )
                    .setApplicationName("data-importer")
                    .build();
            log.info("Google Sheets client initialized successfully");
        } catch (IOException | GeneralSecurityException e) {
            log.error("Failed to initialize Google Sheets client: {}", e.getMessage(), e);
            throw new IllegalStateException("Unable to initialize Google Sheets client", e);
        }
    }

    public List<List<Object>> readSheet(String projectSpreedSheetRange) throws IOException {
        String projectSpreadsheetId = props.getProjectSpreadsheetId();
        log.debug("Reading Google Sheet: id={}, range={}", projectSpreadsheetId, projectSpreedSheetRange);
        ValueRange response = sheetsService.spreadsheets().values()
                .get(projectSpreadsheetId, projectSpreedSheetRange)
                .execute();
        List<List<Object>> values = response.getValues();
        return values == null ? Collections.emptyList() : values;
    }

    public List<List<Object>> readSheet(String spreadsheetId, String range) throws IOException {
        log.debug("Reading Google Sheet: id={}, range={}", spreadsheetId, range);

        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

        List<List<Object>> values = response.getValues();
        return values == null ? Collections.emptyList() : values;
    }

    private GoogleCredentials loadCredentials() throws IOException {
        if (!credentialsJson.isBlank()) {
            log.info("Loading Google credentials from JSON config property");
            return GoogleCredentials.fromStream(
                    new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8))
            );
        }
        throw new IllegalStateException(
                "Google credentials not configured: neither google.credentials.path nor google.credentials.json is set"
        );
    }

    public void addRowsToSheets(
            String spreadSheetId,
            String sheetRange,
            List<List<String>> rows
    ) {
        try {
            List<List<Object>> values = rows.stream()
                    .map(row -> row.stream().map(value -> (Object) value)
                            .toList())
                    .toList();

            ValueRange valueRange = new ValueRange().setValues(values);

            sheetsService.spreadsheets()
                    .values()
                    .append(spreadSheetId, sheetRange, valueRange)
                    .setValueInputOption("USER_ENTERED")
                    .execute();

            log.info("[Sheets] Rows have been successfully added to the google sheet");

        } catch (Exception e) {
            log.error("[Sheets] Failed to append rows spreadsheetId={}, range={}, reason={} ",
                    spreadSheetId,
                    sheetRange,
                    e.getMessage());

            throw new RuntimeException("Failed to append to Google Sheets", e);
        }
    }

    public List<String> getSheetNames(String spreadsheetId) throws IOException {
        Spreadsheet spreadsheet = sheetsService.spreadsheets()
                .get(spreadsheetId)
                .setIncludeGridData(false)
                .execute();

        return spreadsheet.getSheets().stream()
                .map(sheet -> sheet.getProperties().getTitle())
                .toList();
    }

}
