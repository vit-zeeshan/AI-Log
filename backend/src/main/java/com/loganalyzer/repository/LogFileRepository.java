package com.loganalyzer.repository;

import com.loganalyzer.entity.LogFile;
import com.loganalyzer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LogFileRepository extends JpaRepository<LogFile, Long> {
    List<LogFile> findByUserOrderByUploadTimestampDesc(User user);
    Optional<LogFile> findByIdAndUser(Long id, User user);
    long countByUser(User user);
    List<LogFile> findAllByOrderByUploadTimestampDesc();
}
