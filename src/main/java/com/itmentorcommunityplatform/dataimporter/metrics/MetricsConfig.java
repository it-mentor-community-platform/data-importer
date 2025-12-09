package com.itmentorcommunityplatform.dataimporter.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter usersImportSuccessCounter(MeterRegistry registry) {
        return Counter.builder("data_importer_users_success_total")
                .description("Total successfully imported users")
                .register(registry);
    }

    @Bean
    public Counter usersImportErrorCounter(MeterRegistry registry) {
        return Counter.builder("data_importer_users_error_total")
                .description("Total failed user imports")
                .register(registry);
    }

    @Bean
    public Timer usersImportDurationTimer(MeterRegistry registry) {
        return Timer.builder("data_importer_users_import_duration_seconds")
                .description("Time taken to complete users import in seconds")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);
    }

    @Bean
    public Counter profilesImportSuccessCounter(MeterRegistry registry) {
        return Counter.builder("data_importer_profiles_success_total")
                .description("Total successfully imported profiles")
                .register(registry);
    }

    @Bean
    public Counter profilesImportErrorCounter(MeterRegistry registry) {
        return Counter.builder("data_importer_profiles_error_total")
                .description("Total failed profile imports")
                .register(registry);
    }

    @Bean
    public Timer profilesImportDurationTimer(MeterRegistry registry) {
        return Timer.builder("data_importer_profiles_import_duration_seconds")
                .description("Time taken to complete profiles import in seconds")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);
    }

    @Bean
    public Counter projectImportSuccessCounter(MeterRegistry registry) {
        return Counter.builder("data_importer_projects_success_total")
                .description("Total successfully imported projects")
                .register(registry);
    }

    @Bean
    public Counter projectImportErrorCounter(MeterRegistry registry) {
        return Counter.builder("data_importer_projects_error_total")
                .description("Total failed project imports")
                .register(registry);
    }

    @Bean
    public Timer projectImportDurationTimer(MeterRegistry registry) {
        return Timer.builder("data_importer_projects_import_duration_seconds")
                .description("Time taken to complete projects import in seconds")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);
    }
}
