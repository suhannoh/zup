package com.noh.zup.domain.collection;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceWatchRepository extends JpaRepository<SourceWatch, Long> {

    List<SourceWatch> findAllByOrderByIdDesc();
}
