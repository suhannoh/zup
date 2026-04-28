package com.noh.zup.domain.collection;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PageSnapshotRepository extends JpaRepository<PageSnapshot, Long> {

    List<PageSnapshot> findAllBySourceWatchIdOrderByFetchedAtDesc(Long sourceWatchId);
}
