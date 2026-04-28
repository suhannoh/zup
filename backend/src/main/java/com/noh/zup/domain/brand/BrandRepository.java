package com.noh.zup.domain.brand;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findBySlug(String slug);

    Optional<Brand> findBySlugAndIsActiveTrue(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<Brand> findAllByIsActiveTrueOrderByNameAsc();

    List<Brand> findAllByOrderByNameAsc();

    List<Brand> findAllByCategorySlugAndIsActiveTrueOrderByNameAsc(String categorySlug);

    List<Brand> findAllByNameContainingIgnoreCaseAndIsActiveTrueOrderByNameAsc(String keyword);

    List<Brand> findAllByCategorySlugAndNameContainingIgnoreCaseAndIsActiveTrueOrderByNameAsc(
            String categorySlug,
            String keyword
    );

    long countByIsActiveTrue();
}
