package com.itmentorcommunityplatform.dataimporter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserImportDto {
    private Long telegramId;
    private String role;
}
