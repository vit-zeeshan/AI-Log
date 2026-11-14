package com.loganalyzer.repository;

import com.loganalyzer.entity.ErrorCategory;
import com.loganalyzer.entity.LogAnalysis;
import com.loganalyzer.entity.ParsedError;
import com.loganalyzer.entity.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParsedErrorRepository extends JpaRepository<ParsedError, Long> {
    List<ParsedError> findByAnalysisOrderByOccurrenceCountDesc(LogAnalysis analysis);
    
    Page<ParsedError> findByAnalysis(LogAnalysis analysis, Pageable pageable);

    @Query("SELECT pe FROM ParsedError pe WHERE pe.analysis = :analysis " +
           "AND (:severity IS NULL OR pe.severity = :severity) " +
           "AND (:category IS NULL OR pe.category = :category) " +
           "AND (:serviceName IS NULL OR LOWER(pe.serviceName) LIKE LOWER(CONCAT('%', :serviceName, '%'))) " +
           "AND (:query IS NULL OR (LOWER(pe.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(pe.message) LIKE LOWER(CONCAT('%', :query, '%')))) " +
           "ORDER BY pe.occurrenceCount DESC")
    Page<ParsedError> searchErrors(
            @Param("analysis") LogAnalysis analysis,
            @Param("severity") Severity severity,
            @Param("category") ErrorCategory category,
            @Param("serviceName") String serviceName,
            @Param("query") String query,
            Pageable pageable
    );

    @Query("SELECT pe.category, COUNT(pe) FROM ParsedError pe WHERE pe.analysis = :analysis GROUP BY pe.category")
    List<Object[]> countByCategory(@Param("analysis") LogAnalysis analysis);

    @Query("SELECT pe.severity, COUNT(pe) FROM ParsedError pe WHERE pe.analysis = :analysis GROUP BY pe.severity")
    List<Object[]> countBySeverity(@Param("analysis") LogAnalysis analysis);

    @Query("SELECT pe.serviceName, COUNT(pe) FROM ParsedError pe WHERE pe.analysis = :analysis AND pe.serviceName IS NOT NULL GROUP BY pe.serviceName")
    List<Object[]> countByServiceName(@Param("analysis") LogAnalysis analysis);
}
