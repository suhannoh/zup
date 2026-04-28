package com.noh.zup.domain.benefit;

import com.noh.zup.domain.tag.Tag;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "benefit_tags",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_benefit_tags_benefit_id_tag_id",
                columnNames = {"benefit_id", "tag_id"}
        )
)
public class BenefitTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "benefit_id", nullable = false)
    private Benefit benefit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected BenefitTag() {
    }

    public BenefitTag(Benefit benefit, Tag tag) {
        this.benefit = benefit;
        this.tag = tag;
    }

    public Long getId() {
        return id;
    }

    public Benefit getBenefit() {
        return benefit;
    }

    public Tag getTag() {
        return tag;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
