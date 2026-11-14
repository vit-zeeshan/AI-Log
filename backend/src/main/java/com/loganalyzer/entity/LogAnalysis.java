package com.loganalyzer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "log_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_file_id", nullable = false)
    private LogFile logFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_lines")
    @Builder.Default
    private Long totalLines = 0L;

    @Column(name = "total_errors")
    @Builder.Default
    private Integer totalErrors = 0;

    @Column(name = "total_warnings")
    @Builder.Default
    private Integer totalWarnings = 0;

    @Column(name = "critical_errors")
    @Builder.Default
    private Integer criticalErrors = 0;

    @Column(name = "unique_patterns")
    @Builder.Default
    private Integer uniquePatterns = 0;

    @Column(name = "duration_ms")
    @Builder.Default
    private Long durationMs = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private AnalysisStatus status = AnalysisStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ParsedError> errors = new ArrayList<>();
}
