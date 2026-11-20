package com.loganalyzer.service;

import com.loganalyzer.dto.LogDtos;
import com.loganalyzer.entity.FileStatus;
import com.loganalyzer.entity.LogAnalysis;
import com.loganalyzer.entity.LogFile;
import com.loganalyzer.entity.User;
import com.loganalyzer.exception.ResourceNotFoundException;
import com.loganalyzer.repository.LogAnalysisRepository;
import com.loganalyzer.repository.LogFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LogFileService {

    private final LogFileRepository logFileRepository;
    private final LogAnalysisRepository logAnalysisRepository;
    private final FileStorageService fileStorageService;

    public LogFileService(LogFileRepository logFileRepository,
                          LogAnalysisRepository logAnalysisRepository,
                          FileStorageService fileStorageService) {
        this.logFileRepository = logFileRepository;
        this.logAnalysisRepository = logAnalysisRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public LogDtos.LogFileDto uploadLogFile(MultipartFile file, User user) {
        FileStorageService.StoredFile stored = fileStorageService.storeFile(file);
        LogFile logFile = LogFile.builder()
                .user(user)
                .filename(stored.storedFilename())
                .originalFilename(stored.originalFilename())
                .fileSize(stored.size())
                .fileFormat(stored.format())
                .storagePath(stored.absolutePath())
                .status(FileStatus.UPLOADED)
                .checksum(stored.checksum())
                .build();
        logFileRepository.save(logFile);
        return mapToDto(logFile, null);
    }

    @Transactional(readOnly = true)
    public List<LogDtos.LogFileDto> getAllFiles() {
        List<LogFile> files = logFileRepository.findAllByOrderByUploadTimestampDesc();
        return files.stream()
                .map(f -> {
                    Optional<LogAnalysis> latest = logAnalysisRepository.findFirstByLogFileOrderByCreatedAtDesc(f);
                    return mapToDto(f, latest.map(LogAnalysis::getId).orElse(null));
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LogDtos.LogFileDto> getUserFiles(User user) {
        // If the user has ROLE_ADMIN, let them see and manage all uploaded files across the system
        List<LogFile> files = "ROLE_ADMIN".equals(user.getRole())
                ? logFileRepository.findAllByOrderByUploadTimestampDesc()
                : logFileRepository.findByUserOrderByUploadTimestampDesc(user);

        return files.stream()
                .map(f -> {
                    Optional<LogAnalysis> latest = logAnalysisRepository.findFirstByLogFileOrderByCreatedAtDesc(f);
                    return mapToDto(f, latest.map(LogAnalysis::getId).orElse(null));
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LogFile getLogFile(Long id, User user) {
        if ("ROLE_ADMIN".equals(user.getRole())) {
            return logFileRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Log file", "id", id));
        }
        return logFileRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Log file", "id", id));
    }

    @Transactional
    public void deleteFile(Long id, User user) {
        LogFile logFile = getLogFile(id, user);
        fileStorageService.deleteFile(logFile.getStoragePath());
        logFileRepository.delete(logFile);
    }

    public LogDtos.LogFileDto mapToDto(LogFile file, Long latestAnalysisId) {
        return LogDtos.LogFileDto.builder()
                .id(file.getId())
                .filename(file.getFilename())
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getFileSize())
                .fileFormat(file.getFileFormat())
                .status(file.getStatus())
                .checksum(file.getChecksum())
                .uploadTimestamp(file.getUploadTimestamp())
                .latestAnalysisId(latestAnalysisId)
                .build();
    }
}
