package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.dto.request.GuaranteedReviewRequestDto;

record GuaranteedReviewImportRequest(
        GuaranteedReviewRequestDto request,
        Long mentorTelegramId
) {
}