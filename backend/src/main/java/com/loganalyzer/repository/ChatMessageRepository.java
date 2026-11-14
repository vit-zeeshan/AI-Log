package com.loganalyzer.repository;

import com.loganalyzer.entity.ChatMessage;
import com.loganalyzer.entity.LogFile;
import com.loganalyzer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByLogFileAndUserOrderByTimestampAsc(LogFile logFile, User user);
    void deleteByLogFile(LogFile logFile);
}
