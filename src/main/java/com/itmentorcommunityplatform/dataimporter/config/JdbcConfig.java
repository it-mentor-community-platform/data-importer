package com.itmentorcommunityplatform.dataimporter.config;

import com.itmentorcommunityplatform.dataimporter.model.ImportType;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.core.mapping.JdbcValue;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

import java.sql.JDBCType;
import java.util.List;

@Configuration
public class JdbcConfig extends AbstractJdbcConfiguration {

    @Override
    protected List<?> userConverters() {
        return List.of(new ImportTypeWriteConverter(), new ImportTypeReadConverter());
    }

    @WritingConverter
    private static class ImportTypeWriteConverter implements Converter<ImportType, JdbcValue> {
        @Override
        public JdbcValue convert(ImportType type) {
            return JdbcValue.of(type.getId(), JDBCType.BIGINT);
        }
    }

    @ReadingConverter
    private static class ImportTypeReadConverter implements Converter<Long, ImportType> {
        @Override
        public ImportType convert(Long typeId) {
            return ImportType.fromId(typeId);
        }
    }
}