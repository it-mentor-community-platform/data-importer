package com.itmentorcommunityplatform.dataimporter.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class ImportMetrics {

    private final Counter importSuccessCounter;
    private final Counter importErrorCounter;
    private final Timer importDurationTimer;
    private final Counter importUniqueUsersCounter;

    public ImportMetrics(MeterRegistry meterRegistry) {
        this.importSuccessCounter = Counter.builder("data_importer_users_success_total")
                .description("Total successfully imported users (including updates)")
                .register(meterRegistry);

        this.importUniqueUsersCounter = Counter.builder("data_importer_users_unique_total")
                .description("Total unique telegram_user_id processed during import")
                .register(meterRegistry);

        this.importErrorCounter = Counter.builder("data_importer_users_error_total")
                .description("Total failed user imports")
                .register(meterRegistry);

        this.importDurationTimer = Timer.builder("data_importer_users_import_duration_seconds")
                .description("Time taken to complete users import in seconds")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);
    }
}
