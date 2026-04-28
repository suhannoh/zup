package com.noh.zup.domain.collection;

import static org.assertj.core.api.Assertions.assertThat;

import com.noh.zup.domain.brand.Brand;
import com.noh.zup.domain.brand.BrandRepository;
import com.noh.zup.domain.category.Category;
import com.noh.zup.domain.category.CategoryRepository;
import com.noh.zup.domain.source.SourceType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class SourceWatchRepositoryTest {

    @Autowired
    private SourceWatchRepository sourceWatchRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void activeSourceWatchWithNullNextFetchAtIsDue() {
        Brand brand = saveBrand("due-null-brand");
        SourceWatch due = sourceWatchRepository.save(new SourceWatch(
                brand,
                SourceType.OFFICIAL_HOME,
                "Due null",
                "https://example.com/due-null",
                true
        ));

        assertThat(sourceWatchRepository.findDueWatches(LocalDateTime.now(), PageRequest.of(0, 5)))
                .extracting(SourceWatch::getId)
                .contains(due.getId());
    }

    @Test
    void futureNextFetchAtAndInactiveSourceWatchAreNotDue() {
        Brand brand = saveBrand("not-due-brand");
        SourceWatch future = new SourceWatch(
                brand,
                SourceType.OFFICIAL_HOME,
                "Future",
                "https://example.com/future",
                true
        );
        future.updateNextFetchAt(LocalDateTime.now().plusHours(1));
        sourceWatchRepository.save(future);

        SourceWatch inactive = sourceWatchRepository.save(new SourceWatch(
                brand,
                SourceType.OFFICIAL_HOME,
                "Inactive",
                "https://example.com/inactive",
                false
        ));

        assertThat(sourceWatchRepository.findDueWatches(LocalDateTime.now(), PageRequest.of(0, 5)))
                .extracting(SourceWatch::getId)
                .doesNotContain(future.getId(), inactive.getId());
    }

    @Test
    void pastNextFetchAtIsDueAndBatchSizeIsApplied() {
        Brand brand = saveBrand("due-past-brand");
        SourceWatch first = new SourceWatch(
                brand,
                SourceType.OFFICIAL_HOME,
                "Past 1",
                "https://example.com/past-1",
                true
        );
        first.updateNextFetchAt(LocalDateTime.now().minusMinutes(1));
        sourceWatchRepository.save(first);

        SourceWatch second = new SourceWatch(
                brand,
                SourceType.OFFICIAL_HOME,
                "Past 2",
                "https://example.com/past-2",
                true
        );
        second.updateNextFetchAt(LocalDateTime.now().minusMinutes(1));
        sourceWatchRepository.save(second);

        assertThat(sourceWatchRepository.findDueWatches(LocalDateTime.now(), PageRequest.of(0, 1)))
                .hasSize(1)
                .extracting(SourceWatch::getId)
                .containsExactly(first.getId());
    }

    private Brand saveBrand(String slug) {
        Category category = categoryRepository.save(new Category("Test Category", "test-" + slug, 1));
        return brandRepository.save(new Brand(category, "Test Brand " + slug, slug));
    }
}
