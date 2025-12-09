package com.itmentorcommunityplatform.dataimporter.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfileDetailsDto {
    private String githubProfile;
    private String telegramUrl;
}
