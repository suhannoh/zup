package com.noh.zup.domain.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.noh.zup.domain.brand.BrandRepository;
import com.noh.zup.domain.collection.candidate.BenefitCandidateDetector;
import com.noh.zup.domain.collection.extract.HtmlTextExtractor;
import com.noh.zup.domain.collection.fetch.OfficialSourceFetcher;
import com.noh.zup.domain.collection.robots.RobotsTxtChecker;
import com.noh.zup.domain.collection.scheduler.CollectionLock;
import com.noh.zup.domain.collection.scheduler.CollectionRedisLock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SourceWatchServiceTest {

    private final SourceWatchRepository sourceWatchRepository = mock(SourceWatchRepository.class);
    private final PageSnapshotRepository pageSnapshotRepository = mock(PageSnapshotRepository.class);
    private final BrandRepository brandRepository = mock(BrandRepository.class);
    private final CollectionRunRepository collectionRunRepository = mock(CollectionRunRepository.class);
    private final OfficialSourceFetcher officialSourceFetcher = mock(OfficialSourceFetcher.class);
    private final RobotsTxtChecker robotsTxtChecker = mock(RobotsTxtChecker.class);
    private final HtmlTextExtractor htmlTextExtractor = mock(HtmlTextExtractor.class);
    private final BenefitCandidateDetector benefitCandidateDetector = mock(BenefitCandidateDetector.class);
    private final CollectionRedisLock collectionRedisLock = mock(CollectionRedisLock.class);

    private SourceWatchService sourceWatchService;

    @BeforeEach
    void setUp() {
        sourceWatchService = new SourceWatchService(
                sourceWatchRepository,
                pageSnapshotRepository,
                brandRepository,
                collectionRunRepository,
                officialSourceFetcher,
                robotsTxtChecker,
                htmlTextExtractor,
                benefitCandidateDetector,
                collectionRedisLock,
                60,
                120
        );
        when(collectionRunRepository.save(any(CollectionRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void collectSkipsWhenSameDomainWasCollectedRecently() {
        SourceWatch targetSourceWatch = mock(SourceWatch.class);
        when(targetSourceWatch.getId()).thenReturn(1L);
        when(targetSourceWatch.getIsActive()).thenReturn(true);
        when(targetSourceWatch.getUrl()).thenReturn("https://m.cjone.com/events/birthday");
        when(sourceWatchRepository.findById(1L)).thenReturn(Optional.of(targetSourceWatch));
        when(collectionRedisLock.tryLock(anyLong(), any(Duration.class)))
                .thenReturn(Optional.of(new CollectionLock("lock:1", "token")));

        SourceWatch recentSourceWatch = mock(SourceWatch.class);
        when(recentSourceWatch.getUrl()).thenReturn("https://m.cjone.com/cjmmobile/benefit");
        CollectionRun recentRun = new CollectionRun(recentSourceWatch, CollectionTriggerType.MANUAL);
        recentRun.completeSuccess(true, false, 1);
        when(collectionRunRepository.findRecentByStatuses(any(), any())).thenReturn(List.of(recentRun));

        SourceWatchCollectResponse response = sourceWatchService.collect(1L, CollectionTriggerType.MANUAL);

        assertThat(response.fetched()).isFalse();
        assertThat(response.failureReason()).isEqualTo("RATE_LIMITED_BY_DOMAIN");
        assertThat(response.message()).isEqualTo("같은 도메인의 최근 수집 이후 최소 수집 간격이 지나지 않아 수집을 건너뛰었습니다.");
        verify(robotsTxtChecker, never()).check(any());
        verify(officialSourceFetcher, never()).fetch(any());
    }

    @Test
    void collectSkipsWhenSourceWatchLockCannotBeAcquired() {
        SourceWatch sourceWatch = mock(SourceWatch.class);
        when(sourceWatch.getId()).thenReturn(2L);
        when(sourceWatchRepository.findById(2L)).thenReturn(Optional.of(sourceWatch));
        when(collectionRedisLock.tryLock(anyLong(), any(Duration.class))).thenReturn(Optional.empty());

        SourceWatchCollectResponse response = sourceWatchService.collect(2L, CollectionTriggerType.MANUAL);

        assertThat(response.fetched()).isFalse();
        assertThat(response.failureReason()).isEqualTo("COLLECTION_ALREADY_RUNNING");
        assertThat(response.message()).isEqualTo("같은 SourceWatch 수집이 이미 진행 중입니다.");
        verify(robotsTxtChecker, never()).check(any());
        verify(officialSourceFetcher, never()).fetch(any());
    }
}
