package com.itmentorcommunityplatform.dataimporter.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImportType {
    USERS(1L),
    PROFILES(2L),
    PROJECTS(3L),
    REVIEWS(4L),
    MENTORS(5L),
    GUARANTEED_REVIEWS(6L),
    QUESTIONS(7L);

    private final Long id;

    public static ImportType fromId(long id) {
        for (ImportType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown import type id: %d".formatted(id));
    }
}