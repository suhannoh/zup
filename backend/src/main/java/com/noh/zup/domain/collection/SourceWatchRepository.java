package com.noh.zup.domain.collection;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SourceWatchRepository extends JpaRepository<SourceWatch, Long> {

    List<SourceWatch> findAllByOrderByIdDesc();

    @Query("""
            select sourceWatch
            from SourceWatch sourceWatch
            where sourceWatch.isActive = true
              and (sourceWatch.nextFetchAt is null or sourceWatch.nextFetchAt <= :now)
            order by sourceWatch.id asc
            """)
    List<SourceWatch> findDueWatches(@Param("now") LocalDateTime now, Pageable pageable);
}
