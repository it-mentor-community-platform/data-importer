package com.itmentorcommunityplatform.dataimporter.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GuaranteedReviewRequestDto {

    @JsonProperty("telegram_url")
    private String telegramUrl;

    private String language;

    @JsonProperty("project_type")
    private String projectType;

    @JsonProperty("price_usd")
    private Integer priceUsd;
}
