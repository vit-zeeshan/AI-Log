package com.loganalyzer.repository;

import com.loganalyzer.entity.LogAnalysis;
import com.loganalyzer.entity.ParsedError;
import com.loganalyzer.entity.RootCauseReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RootCauseReportRepository extends JpaRepository<RootCauseReport, Long> {
    Optional<RootCauseReport> findByParsedError(ParsedError parsedError);
    List<RootCauseReport> findByAnalysis(LogAnalysis analysis);
}
