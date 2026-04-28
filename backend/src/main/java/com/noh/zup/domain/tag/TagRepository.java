package com.noh.zup.domain.tag;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findBySlug(String slug);

    Optional<Tag> findBySlugAndIsActiveTrue(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<Tag> findAllByIsActiveTrueOrderByDisplayOrderAscNameAsc();

    List<Tag> findAllByOrderByDisplayOrderAscNameAsc();
}
