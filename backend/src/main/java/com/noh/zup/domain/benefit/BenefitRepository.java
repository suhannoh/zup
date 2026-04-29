package com.noh.zup.domain.benefit;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BenefitRepository extends JpaRepository<Benefit, Long> {

    Optional<Benefit> findByIdAndIsActiveTrue(Long id);

    Optional<Benefit> findByIdAndIsActiveTrueAndVerificationStatus(Long id, VerificationStatus verificationStatus);

    List<Benefit> findAllByBrandSlugAndIsActiveTrueOrderByCreatedAtDesc(String brandSlug);

    List<Benefit> findAllByBrandSlugAndIsActiveTrueAndVerificationStatusOrderByCreatedAtDesc(
            String brandSlug,
            VerificationStatus verificationStatus
    );

    List<Benefit> findAllByVerificationStatusAndIsActiveTrueOrderByUpdatedAtDesc(VerificationStatus status);

    List<Benefit> findAllByIsActiveTrueOrderByCreatedAtDesc();

    List<Benefit> findAllByOrderByUpdatedAtDesc();

    @Query(
            value = """
                    select b
                    from Benefit b
                    join fetch b.brand brand
                    join fetch brand.category category
                    where (:brandSlug is null or brand.slug = :brandSlug)
                      and (:categorySlug is null or category.slug = :categorySlug)
                      and (:verificationStatus is null or b.verificationStatus = :verificationStatus)
                      and (:benefitType is null or b.benefitType = :benefitType)
                      and (:birthdayTimingType is null or b.birthdayTimingType = :birthdayTimingType)
                      and (:isActive is null or b.isActive = :isActive)
                      and (
                        :keyword is null
                        or lower(b.title) like concat('%', :keyword, '%')
                        or lower(b.summary) like concat('%', :keyword, '%')
                        or lower(brand.name) like concat('%', :keyword, '%')
                      )
                    order by b.updatedAt desc, b.id desc
                    """,
            countQuery = """
                    select count(b)
                    from Benefit b
                    join b.brand brand
                    join brand.category category
                    where (:brandSlug is null or brand.slug = :brandSlug)
                      and (:categorySlug is null or category.slug = :categorySlug)
                      and (:verificationStatus is null or b.verificationStatus = :verificationStatus)
                      and (:benefitType is null or b.benefitType = :benefitType)
                      and (:birthdayTimingType is null or b.birthdayTimingType = :birthdayTimingType)
                      and (:isActive is null or b.isActive = :isActive)
                      and (
                        :keyword is null
                        or lower(b.title) like concat('%', :keyword, '%')
                        or lower(b.summary) like concat('%', :keyword, '%')
                        or lower(brand.name) like concat('%', :keyword, '%')
                      )
                    """
    )
    Page<Benefit> searchAdminBenefits(
            @Param("brandSlug") String brandSlug,
            @Param("categorySlug") String categorySlug,
            @Param("verificationStatus") VerificationStatus verificationStatus,
            @Param("benefitType") BenefitType benefitType,
            @Param("birthdayTimingType") BirthdayTimingType birthdayTimingType,
            @Param("isActive") Boolean isActive,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    long countByVerificationStatus(VerificationStatus verificationStatus);

    long countByVerificationStatusAndIsActiveTrue(VerificationStatus verificationStatus);

    @Query("""
            select b
            from Benefit b
            join b.brand brand
            join brand.category category
            where b.isActive = true
              and b.verificationStatus = :publishedStatus
              and (:brandSlug is null or brand.slug = :brandSlug)
              and (:categorySlug is null or category.slug = :categorySlug)
              and (:benefitType is null or b.benefitType = :benefitType)
              and (:birthdayTimingType is null or b.birthdayTimingType = :birthdayTimingType)
              and (:requiredApp is null or b.requiredApp = :requiredApp)
              and (:requiredMembership is null or b.requiredMembership = :requiredMembership)
              and (:requiredPurchase is null or b.requiredPurchase = :requiredPurchase)
              and (
                :tagSlug is null
                or exists (
                  select 1
                  from BenefitTag benefitTag
                  where benefitTag.benefit = b
                    and benefitTag.tag.slug = :tagSlug
                )
              )
            order by b.createdAt desc
            """)
    List<Benefit> searchPublishedBenefits(
            @Param("publishedStatus") VerificationStatus publishedStatus,
            @Param("brandSlug") String brandSlug,
            @Param("categorySlug") String categorySlug,
            @Param("tagSlug") String tagSlug,
            @Param("benefitType") BenefitType benefitType,
            @Param("birthdayTimingType") BirthdayTimingType birthdayTimingType,
            @Param("requiredApp") Boolean requiredApp,
            @Param("requiredMembership") Boolean requiredMembership,
            @Param("requiredPurchase") Boolean requiredPurchase
    );

    List<Benefit> findAllByVerificationStatusInOrderByUpdatedAtDesc(List<VerificationStatus> verificationStatuses);

    List<Benefit> findAllByIsActiveTrueAndVerificationStatusIn(List<VerificationStatus> verificationStatuses);
}
