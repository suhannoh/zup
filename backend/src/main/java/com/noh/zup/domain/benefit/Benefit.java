package com.noh.zup.domain.benefit;

import com.noh.zup.common.entity.BaseTimeEntity;
import com.noh.zup.domain.brand.Brand;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "benefits")
public class Benefit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(columnDefinition = "text")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BenefitType benefitType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OccasionType occasionType = OccasionType.BIRTHDAY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BirthdayTimingType birthdayTimingType = BirthdayTimingType.UNKNOWN;

    @Column(length = 500)
    private String conditionSummary;

    @Column(nullable = false)
    private Boolean requiredApp = false;

    @Column(nullable = false)
    private Boolean requiredMembership = false;

    @Column(nullable = false)
    private Boolean requiredPurchase = false;

    @Column(length = 100)
    private String membershipGrade;

    @Column(length = 500)
    private String usagePeriodDescription;

    private LocalDate availableFrom;

    private LocalDate availableTo;

    @Column(columnDefinition = "text")
    private String caution;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private VerificationStatus verificationStatus = VerificationStatus.DRAFT;

    private LocalDate lastVerifiedAt;

    @Column(nullable = false)
    private Boolean isActive = true;

    protected Benefit() {
    }

    public Benefit(Brand brand, String title, String summary, BenefitType benefitType) {
        this.brand = brand;
        this.title = title;
        this.summary = summary;
        this.benefitType = benefitType;
    }

    public void update(
            Brand brand,
            String title,
            String summary,
            String detail,
            BenefitType benefitType,
            OccasionType occasionType,
            BirthdayTimingType birthdayTimingType,
            String conditionSummary,
            Boolean requiredApp,
            Boolean requiredMembership,
            Boolean requiredPurchase,
            String membershipGrade,
            String usagePeriodDescription,
            LocalDate availableFrom,
            LocalDate availableTo,
            String caution,
            VerificationStatus verificationStatus,
            LocalDate lastVerifiedAt,
            Boolean isActive
    ) {
        if (brand != null) {
            this.brand = brand;
        }
        if (title != null) {
            this.title = title;
        }
        if (summary != null) {
            this.summary = summary;
        }
        if (detail != null) {
            this.detail = detail;
        }
        if (benefitType != null) {
            this.benefitType = benefitType;
        }
        if (occasionType != null) {
            this.occasionType = occasionType;
        }
        if (birthdayTimingType != null) {
            this.birthdayTimingType = birthdayTimingType;
        }
        if (conditionSummary != null) {
            this.conditionSummary = conditionSummary;
        }
        if (requiredApp != null) {
            this.requiredApp = requiredApp;
        }
        if (requiredMembership != null) {
            this.requiredMembership = requiredMembership;
        }
        if (requiredPurchase != null) {
            this.requiredPurchase = requiredPurchase;
        }
        if (membershipGrade != null) {
            this.membershipGrade = membershipGrade;
        }
        if (usagePeriodDescription != null) {
            this.usagePeriodDescription = usagePeriodDescription;
        }
        if (availableFrom != null) {
            this.availableFrom = availableFrom;
        }
        if (availableTo != null) {
            this.availableTo = availableTo;
        }
        if (caution != null) {
            this.caution = caution;
        }
        if (verificationStatus != null) {
            this.verificationStatus = verificationStatus;
        }
        if (lastVerifiedAt != null) {
            this.lastVerifiedAt = lastVerifiedAt;
        }
        if (isActive != null) {
            this.isActive = isActive;
        }
    }

    public void changeStatus(VerificationStatus verificationStatus, LocalDate lastVerifiedAt) {
        this.verificationStatus = verificationStatus;
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public void changeActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Long getId() {
        return id;
    }

    public Brand getBrand() {
        return brand;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getDetail() {
        return detail;
    }

    public BenefitType getBenefitType() {
        return benefitType;
    }

    public OccasionType getOccasionType() {
        return occasionType;
    }

    public BirthdayTimingType getBirthdayTimingType() {
        return birthdayTimingType;
    }

    public String getConditionSummary() {
        return conditionSummary;
    }

    public Boolean getRequiredApp() {
        return requiredApp;
    }

    public Boolean getRequiredMembership() {
        return requiredMembership;
    }

    public Boolean getRequiredPurchase() {
        return requiredPurchase;
    }

    public String getMembershipGrade() {
        return membershipGrade;
    }

    public String getUsagePeriodDescription() {
        return usagePeriodDescription;
    }

    public LocalDate getAvailableFrom() {
        return availableFrom;
    }

    public LocalDate getAvailableTo() {
        return availableTo;
    }

    public String getCaution() {
        return caution;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public LocalDate getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }
}
