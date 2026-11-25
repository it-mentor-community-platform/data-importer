package com.itmentorcommunityplatform.dataimporter.util;

import lombok.extern.slf4j.Slf4j;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Locale;

@Slf4j
public final class SheetMonthTimestampParser {

    private static final String[] RU_MONTHS = {
            "январь", "февраль", "март", "апрель", "май", "июнь",
            "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"
    };

    private SheetMonthTimestampParser() {
    }

    public static Long parseEpochSeconds(String date) {
        if (date.isEmpty()) {
            return null;
        }

        String normalizedDate = date.replace('\u00A0', ' ').trim();

        try {
            DateFormatSymbols symbols = new DateFormatSymbols();
            symbols.setMonths(RU_MONTHS);

            SimpleDateFormat formatter = new SimpleDateFormat("MMMM, yyyy", Locale.forLanguageTag("ru"));
            formatter.setDateFormatSymbols(symbols);

            return formatter.parse(normalizedDate).toInstant().getEpochSecond();
        } catch (Exception e) {
            log.warn("Failed to parse date '{}': {}", normalizedDate, e.getMessage());
            return Instant.now().getEpochSecond();
        }
    }
}