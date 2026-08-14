package com.itmentorcommunityplatform.dataimporter.util;

import org.jspecify.annotations.NonNull;

public final class TelegramLinkUtil {

    public static final String TELEGRAM_LINK_PREFIX = "https://t.me/";
    public static final String PREFIX_TG_USERNAME = "@";


    private TelegramLinkUtil() {
    }

    public static @NonNull String buildTelegramUrl(String tgUsername) {

        if (tgUsername == null || tgUsername.isBlank()) {
            throw new IllegalArgumentException("tgUsername must not be blank");
        }

        if(tgUsername.contains(PREFIX_TG_USERNAME)){
            tgUsername = tgUsername.substring(tgUsername.indexOf(PREFIX_TG_USERNAME) + 1);
        }

        return TELEGRAM_LINK_PREFIX + tgUsername;
    }

}
