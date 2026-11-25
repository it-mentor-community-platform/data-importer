package com.itmentorcommunityplatform.dataimporter.config;

import com.itmentorcommunityplatform.dataimporter.model.ImportType;
import com.itmentorcommunityplatform.dataimporter.service.ImportCommand;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class ImportCommandConfig {

    @Bean
    public Map<ImportType, ImportCommand> importCommands(List<ImportCommand> commands) {
        return commands.stream()
                .collect(Collectors.toMap(ImportCommand::getImportType, Function.identity()));
    }
}