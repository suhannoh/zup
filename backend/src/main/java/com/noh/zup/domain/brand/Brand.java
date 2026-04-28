package com.noh.zup.domain.brand;

import com.noh.zup.common.entity.BaseTimeEntity;
import com.noh.zup.domain.category.Category;

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
@Table(name = "brands")
public class Brand extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String officialUrl;

    @Column(length = 500)
    private String membershipUrl;

    @Column(length = 500)
    private String appUrl;

    @Column(length = 30)
    private String brandColor;

    @Column(length = 500)
    private String logoUrl;

    @Column(nullable = false)
    private Boolean isActive = true;

    protected Brand() {
    }

    public Brand(Category category, String name, String slug) {
        this.category = category;
        this.name = name;
        this.slug = slug;
    }

    public void update(
            Category category,
            String name,
            String slug,
            String description,
            String officialUrl,
            String membershipUrl,
            String appUrl,
            String brandColor,
            String logoUrl,
            Boolean isActive
    ) {
        if (category != null) {
            this.category = category;
        }
        if (name != null) {
            this.name = name;
        }
        if (slug != null) {
            this.slug = slug;
        }
        if (description != null) {
            this.description = description;
        }
        if (officialUrl != null) {
            this.officialUrl = officialUrl;
        }
        if (membershipUrl != null) {
            this.membershipUrl = membershipUrl;
        }
        if (appUrl != null) {
            this.appUrl = appUrl;
        }
        if (brandColor != null) {
            this.brandColor = brandColor;
        }
        if (logoUrl != null) {
            this.logoUrl = logoUrl;
        }
        if (isActive != null) {
            this.isActive = isActive;
        }
    }

    public void changeActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public String getOfficialUrl() {
        return officialUrl;
    }

    public String getMembershipUrl() {
        return membershipUrl;
    }

    public String getAppUrl() {
        return appUrl;
    }

    public String getBrandColor() {
        return brandColor;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public Boolean getIsActive() {
        return isActive;
    }
}
