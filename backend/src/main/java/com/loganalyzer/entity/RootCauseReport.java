package com.loganalyzer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "root_cause_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RootCauseReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parsed_error_id", nullable = false, unique = true)
    private ParsedError parsedError;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private LogAnalysis analysis;

    @Column(name = "what_went_wrong", nullable = false, columnDefinition = "TEXT")
    private String whatWentWrong;

    @Column(name = "why_it_happened", nullable = false, columnDefinition = "TEXT")
    private String whyItHappened;

    @Column(name = "affected_component", nullable = false)
    private String affectedComponent;

    @Column(name = "impact_assessment", nullable = false, columnDefinition = "TEXT")
    private String impactAssessment;

    @Column(name = "primary_or_secondary", nullable = false, length = 50)
    private String primaryOrSecondary; // "PRIMARY" or "SECONDARY"

    @Column(name = "supporting_evidence", nullable = false, columnDefinition = "TEXT")
    private String supportingEvidence;

    @Column(name = "immediate_fix", nullable = false, columnDefinition = "TEXT")
    private String immediateFix;

    @Column(name = "step_by_step_debugging", nullable = false, columnDefinition = "TEXT")
    private String stepByStepDebugging;

    @Column(name = "suggested_code_change", columnDefinition = "TEXT")
    private String suggestedCodeChange;

    @Column(name = "troubleshooting_commands", columnDefinition = "TEXT")
    private String troubleshootingCommands;

    @Column(name = "long_term_prevention", nullable = false, columnDefinition = "TEXT")
    private String longTermPrevention;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
