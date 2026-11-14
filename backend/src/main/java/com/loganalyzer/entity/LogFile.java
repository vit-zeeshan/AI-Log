package com.loganalyzer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String filename;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_format", nullable = false, length = 50)
    private String fileFormat;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private FileStatus status = FileStatus.UPLOADED;

    @Column(length = 100)
    private String checksum;

    @CreationTimestamp
    @Column(name = "upload_timestamp", updatable = false)
    private LocalDateTime uploadTimestamp;
}
