package com.itmentorcommunityplatform.dataimporter.google;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GoogleSheetsClient {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private final DataImporterProperties props;

    public List<List<Object>> readSheet() throws Exception {
        Sheets service = getSheetsService();
        String spreadsheetId = props.getSpreadsheetId();
        String range = props.getSheetRange();

        ValueRange response = service.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

        List<List<Object>> values = response.getValues();
        return values == null ? Collections.emptyList() : values;
    }

    private Sheets getSheetsService() throws Exception {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/spreadsheets.readonly"));
        return new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JSON_FACTORY,
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("data-importer")
                    .build();
    }
}
