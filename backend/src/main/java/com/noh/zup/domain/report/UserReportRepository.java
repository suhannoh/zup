package com.noh.zup.domain.report;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserReportRepository extends JpaRepository<UserReport, Long> {

    List<UserReport> findAllByOrderByCreatedAtDesc();

    long countByStatus(ReportStatus status);
}
