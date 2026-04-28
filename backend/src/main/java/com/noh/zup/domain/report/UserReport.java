package com.noh.zup.domain.report;

import com.noh.zup.common.entity.BaseTimeEntity;
import com.noh.zup.domain.benefit.Benefit;
import com.noh.zup.domain.brand.Brand;
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
import java.time.LocalDateTime;

@Entity
@Table(name = "user_reports")
public class UserReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benefit_id")
    private Benefit benefit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReportType reportType;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(length = 1000)
    private String referenceUrl;

    @Column(length = 255)
    private String email;

    @Column(columnDefinition = "text")
    private String adminMemo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReportStatus status = ReportStatus.RECEIVED;

    private LocalDateTime resolvedAt;

    protected UserReport() {
    }

    public UserReport(
            Brand brand,
            Benefit benefit,
            ReportType reportType,
            String content,
            String referenceUrl,
            String email
    ) {
        this.brand = brand;
        this.benefit = benefit;
        this.reportType = reportType;
        this.content = content;
        this.referenceUrl = referenceUrl;
        this.email = email;
    }

    public void changeStatus(ReportStatus status) {
        this.status = status;
        if (status == ReportStatus.RESOLVED || status == ReportStatus.REJECTED) {
            this.resolvedAt = LocalDateTime.now();
            return;
        }
        this.resolvedAt = null;
    }

    public void updateAdminMemo(String adminMemo) {
        this.adminMemo = adminMemo;
    }

    public Long getId() {
        return id;
    }

    public Brand getBrand() {
        return brand;
    }

    public Benefit getBenefit() {
        return benefit;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public String getContent() {
        return content;
    }

    public String getReferenceUrl() {
        return referenceUrl;
    }

    public String getEmail() {
        return email;
    }

    public String getAdminMemo() {
        return adminMemo;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
}
