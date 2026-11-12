package com.itmentorcommunityplatform.dataimporter.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
@Table("import_execution")
public class ImportExecution {

    @Id
    private Long id;

    @Column("type")
    private Long typeId;

    private ImportExecutionStatus status;

    private Long startedAt;

    private Long finishedAt;

    @Builder.Default
    private int successCount = 0;

    @Builder.Default
    private int errorCount = 0;

    private SourceReadStatus sourceReadStatus;

    public void completeSuccessfully(int successCount, int errorCount) {
        ensureNotFinished();
        this.status = ImportExecutionStatus.SUCCESS;
        this.sourceReadStatus = SourceReadStatus.SUCCESS;
        this.successCount = successCount;
        this.errorCount = errorCount;
        this.finishedAt = Instant.now().getEpochSecond();
    }

    public void completeFailed(int successCount, int errorCount, SourceReadStatus readStatus) {
        ensureNotFinished();
        this.status = ImportExecutionStatus.FAILED;
        this.sourceReadStatus = readStatus;
        this.successCount = successCount;
        this.errorCount = errorCount;
        this.finishedAt = Instant.now().getEpochSecond();
    }

    public void recordSuccess() {
        ensureNotFinished();
        this.successCount++;
    }

    public void recordFailure() {
        ensureNotFinished();
        this.errorCount++;
    }

    private void ensureNotFinished() {
        if (finishedAt != null) {
            throw new IllegalStateException(
                    "Import execution %d is already finished with status %s".formatted(id, status)
            );
        }
    }
}