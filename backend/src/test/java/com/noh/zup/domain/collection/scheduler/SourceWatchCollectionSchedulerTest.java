package com.noh.zup.domain.collection.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.noh.zup.domain.collection.SourceWatch;
import com.noh.zup.domain.collection.SourceWatchCollectResponse;
import com.noh.zup.domain.collection.SourceWatchRepository;
import com.noh.zup.domain.collection.SourceWatchService;
import com.noh.zup.domain.collection.CollectionTriggerType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

class SourceWatchCollectionSchedulerTest {

    private final SourceWatchRepository sourceWatchRepository = mock(SourceWatchRepository.class);
    private final SourceWatchService sourceWatchService = mock(SourceWatchService.class);
    private final CollectionRedisLock collectionRedisLock = mock(CollectionRedisLock.class);
    private final SourceWatchCollectionScheduler scheduler = new SourceWatchCollectionScheduler(
            sourceWatchRepository,
            sourceWatchService,
            collectionRedisLock,
            5,
            120,
            1440,
            180
    );

    @Test
    void lockFailureSkipsCollect() {
        SourceWatch sourceWatch = mock(SourceWatch.class);
        when(sourceWatch.getId()).thenReturn(1L);
        when(sourceWatchRepository.findDueWatches(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(sourceWatch));
        when(collectionRedisLock.tryLock(eq(1L), eq(Duration.ofSeconds(120))))
                .thenReturn(Optional.empty());

        scheduler.runDueCollectionTick();

        verify(sourceWatchService, never()).collect(eq(1L), any(CollectionTriggerType.class));
    }

    @Test
    void collectSuccessUpdatesNextFetchAtWithDefaultIntervalAndReleasesLock() {
        CollectionLock lock = new CollectionLock("key", "token");
        SourceWatch sourceWatch = mock(SourceWatch.class);
        when(sourceWatch.getId()).thenReturn(2L);
        when(sourceWatchRepository.findDueWatches(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(sourceWatch));
        when(collectionRedisLock.tryLock(eq(2L), eq(Duration.ofSeconds(120))))
                .thenReturn(Optional.of(lock));
        when(sourceWatchService.collect(2L, CollectionTriggerType.SCHEDULED))
                .thenReturn(new SourceWatchCollectResponse(2L, true, false, 1, null, "collection completed"));

        scheduler.runDueCollectionTick();

        ArgumentCaptor<LocalDateTime> nextFetchAt = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(sourceWatchService).updateNextFetchAt(eq(2L), nextFetchAt.capture());
        verify(collectionRedisLock).release(lock);
    }

    @Test
    void collectFailureUpdatesNextFetchAtWithRetryIntervalAndReleasesLock() {
        CollectionLock lock = new CollectionLock("key", "token");
        SourceWatch sourceWatch = mock(SourceWatch.class);
        when(sourceWatch.getId()).thenReturn(3L);
        when(sourceWatchRepository.findDueWatches(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(sourceWatch));
        when(collectionRedisLock.tryLock(eq(3L), eq(Duration.ofSeconds(120))))
                .thenReturn(Optional.of(lock));
        when(sourceWatchService.collect(3L, CollectionTriggerType.SCHEDULED))
                .thenReturn(new SourceWatchCollectResponse(3L, false, false, 0, "FETCH_FAILED", "HTTP status 500"));

        scheduler.runDueCollectionTick();

        verify(sourceWatchService).updateNextFetchAt(eq(3L), any(LocalDateTime.class));
        verify(collectionRedisLock).release(lock);
    }
}
