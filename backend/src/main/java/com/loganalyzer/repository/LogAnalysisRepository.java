package com.loganalyzer.repository;

import com.loganalyzer.entity.LogAnalysis;
import com.loganalyzer.entity.LogFile;
import com.loganalyzer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LogAnalysisRepository extends JpaRepository<LogAnalysis, Long> {
    List<LogAnalysis> findByUserOrderByCreatedAtDesc(User user);
    Optional<LogAnalysis> findByIdAndUser(Long id, User user);
    Optional<LogAnalysis> findFirstByLogFileOrderByCreatedAtDesc(LogFile logFile);
    Optional<LogAnalysis> findFirstByLogFileAndUserOrderByCreatedAtDesc(LogFile logFile, User user);

    @Query("SELECT COUNT(a) FROM LogAnalysis a WHERE a.user = :user")
    long countByUser(@Param("user") User user);

    @Query("SELECT COALESCE(SUM(a.totalErrors), 0) FROM LogAnalysis a WHERE a.user = :user")
    long sumTotalErrorsByUser(@Param("user") User user);

    @Query("SELECT COALESCE(SUM(a.criticalErrors), 0) FROM LogAnalysis a WHERE a.user = :user")
    long sumCriticalErrorsByUser(@Param("user") User user);

    @Query("SELECT COALESCE(SUM(a.totalWarnings), 0) FROM LogAnalysis a WHERE a.user = :user")
    long sumTotalWarningsByUser(@Param("user") User user);
}
