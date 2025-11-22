package com.itmentorcommunityplatform.dataimporter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UserUpsertRequestDto(
        @NotNull
        @JsonProperty("telegram_user_id")
        Long telegramUserId,
        @NotEmpty
        List<String> roles
) {}