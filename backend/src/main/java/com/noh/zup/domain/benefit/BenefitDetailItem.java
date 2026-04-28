package com.noh.zup.domain.benefit;

import com.noh.zup.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "benefit_detail_items")
public class BenefitDetailItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "benefit_id", nullable = false)
    private Benefit benefit;

    @Column(length = 120)
    private String brandName;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String conditionText;

    @Column(length = 1000)
    private String imageUrl;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private Boolean isActive = true;

    protected BenefitDetailItem() {
    }

    public BenefitDetailItem(
            Benefit benefit,
            String brandName,
            String title,
            String description,
            String conditionText,
            String imageUrl,
            Integer displayOrder
    ) {
        this.benefit = benefit;
        this.brandName = brandName;
        this.title = title;
        this.description = description;
        this.conditionText = conditionText;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder == null ? 0 : displayOrder;
        this.isActive = true;
    }

    public void update(
            String brandName,
            String title,
            String description,
            String conditionText,
            String imageUrl,
            Integer displayOrder
    ) {
        if (brandName != null) {
            this.brandName = brandName;
        }
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (conditionText != null) {
            this.conditionText = conditionText;
        }
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
        if (displayOrder != null) {
            this.displayOrder = displayOrder;
        }
    }

    public void changeActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Long getId() {
        return id;
    }

    public Benefit getBenefit() {
        return benefit;
    }

    public String getBrandName() {
        return brandName;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getConditionText() {
        return conditionText;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public Boolean getIsActive() {
        return isActive;
    }
}
