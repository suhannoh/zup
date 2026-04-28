package com.noh.zup.domain.collection.scheduler;

import com.noh.zup.domain.collection.SourceWatch;
import com.noh.zup.domain.collection.SourceWatchCollectResponse;
import com.noh.zup.domain.collection.SourceWatchRepository;
import com.noh.zup.domain.collection.SourceWatchService;
import com.noh.zup.domain.collection.CollectionTriggerType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.collection.scheduler", name = "enabled", havingValue = "true")
public class SourceWatchCollectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SourceWatchCollectionScheduler.class);
    private static final String SUCCESS_MESSAGE = "collection completed";

    private final SourceWatchRepository sourceWatchRepository;
    private final SourceWatchService sourceWatchService;
    private final CollectionRedisLock collectionRedisLock;
    private final int batchSize;
    private final long lockTtlSeconds;
    private final long defaultFetchIntervalMinutes;
    private final long failureRetryMinutes;

    public SourceWatchCollectionScheduler(
            SourceWatchRepository sourceWatchRepository,
            SourceWatchService sourceWatchService,
            CollectionRedisLock collectionRedisLock,
            @Value("${app.collection.scheduler.batch-size:5}") int batchSize,
            @Value("${app.collection.scheduler.lock-ttl-seconds:120}") long lockTtlSeconds,
            @Value("${app.collection.scheduler.default-fetch-interval-minutes:1440}") long defaultFetchIntervalMinutes,
            @Value("${app.collection.scheduler.failure-retry-minutes:180}") long failureRetryMinutes
    ) {
        this.sourceWatchRepository = sourceWatchRepository;
        this.sourceWatchService = sourceWatchService;
        this.collectionRedisLock = collectionRedisLock;
        this.batchSize = batchSize;
        this.lockTtlSeconds = lockTtlSeconds;
        this.defaultFetchIntervalMinutes = defaultFetchIntervalMinutes;
        this.failureRetryMinutes = failureRetryMinutes;
    }

    @Scheduled(fixedDelayString = "${app.collection.scheduler.fixed-delay-ms:60000}")
    public void runDueCollectionTick() {
        LocalDateTime now = LocalDateTime.now();
        List<SourceWatch> dueWatches = sourceWatchRepository.findDueWatches(
                now,
                PageRequest.of(0, Math.max(batchSize, 1))
        );
        log.info("Collection scheduler started. batchSize={} dueCount={}", batchSize, dueWatches.size());

        for (SourceWatch sourceWatch : dueWatches) {
            collectOne(sourceWatch.getId(), now);
        }
    }

    private void collectOne(Long sourceWatchId, LocalDateTime tickStartedAt) {
        Optional<CollectionLock> lock = collectionRedisLock.tryLock(
                sourceWatchId,
                Duration.ofSeconds(lockTtlSeconds)
        );
        if (lock.isEmpty()) {
            log.info("SourceWatch collect skipped by lock. id={}", sourceWatchId);
            return;
        }

        try {
            SourceWatchCollectResponse response = sourceWatchService.collect(sourceWatchId, CollectionTriggerType.SCHEDULED);
            boolean success = SUCCESS_MESSAGE.equals(response.message());
            LocalDateTime nextFetchAt = success
                    ? tickStartedAt.plusMinutes(defaultFetchIntervalMinutes)
                    : tickStartedAt.plusMinutes(failureRetryMinutes);
            sourceWatchService.updateNextFetchAt(sourceWatchId, nextFetchAt);

            if (success) {
                log.info(
                        "SourceWatch collect success. id={} candidateCount={} sameAsPrevious={}",
                        sourceWatchId,
                        response.candidateCount(),
                        response.sameAsPrevious()
                );
            } else {
                log.warn("SourceWatch collect failed. id={} reason={}", sourceWatchId, response.message());
            }
        } catch (Exception exception) {
            LocalDateTime nextFetchAt = tickStartedAt.plusMinutes(failureRetryMinutes);
            try {
                sourceWatchService.markFailedAndUpdateNextFetchAt(sourceWatchId, nextFetchAt);
            } catch (Exception updateException) {
                log.warn("SourceWatch failure state update failed. id={}", sourceWatchId, updateException);
            }
            log.warn("SourceWatch collect failed. id={} reason={}", sourceWatchId, exception.getMessage(), exception);
        } finally {
            collectionRedisLock.release(lock.get());
        }
    }
}
