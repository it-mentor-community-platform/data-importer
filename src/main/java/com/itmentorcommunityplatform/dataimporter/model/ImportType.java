package com.itmentorcommunityplatform.dataimporter.model;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;


@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Table("import_type")
public class ImportType {

    @Id
    private Long id;

    @EqualsAndHashCode.Include
    private String code;

    public ImportType(String code) {
        this.code = code;
    }
}