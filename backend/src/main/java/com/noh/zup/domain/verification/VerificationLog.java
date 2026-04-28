package com.noh.zup.domain.verification;

import com.noh.zup.common.entity.BaseTimeEntity;
import com.noh.zup.domain.benefit.Benefit;
import com.noh.zup.domain.benefit.VerificationStatus;
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
@Table(name = "verification_logs")
public class VerificationLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "benefit_id", nullable = false)
    private Benefit benefit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private VerificationStatus beforeStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private VerificationStatus afterStatus;

    @Column(columnDefinition = "text")
    private String memo;

    @Column(length = 255)
    private String adminEmail;

    @Column(nullable = false)
    private LocalDateTime verifiedAt;

    protected VerificationLog() {
    }

    public VerificationLog(
            Benefit benefit,
            VerificationStatus beforeStatus,
            VerificationStatus afterStatus,
            String memo,
            String adminEmail,
            LocalDateTime verifiedAt
    ) {
        this.benefit = benefit;
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
        this.memo = memo;
        this.adminEmail = adminEmail;
        this.verifiedAt = verifiedAt;
    }

    public Long getId() {
        return id;
    }

    public Benefit getBenefit() {
        return benefit;
    }

    public VerificationStatus getBeforeStatus() {
        return beforeStatus;
    }

    public VerificationStatus getAfterStatus() {
        return afterStatus;
    }

    public String getMemo() {
        return memo;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }
}
