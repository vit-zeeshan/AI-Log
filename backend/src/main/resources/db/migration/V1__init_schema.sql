-- Flyway Migration V1: Initial Schema for AI-Powered Log Analyzer

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS log_files (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_format VARCHAR(50) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
    checksum VARCHAR(100),
    upload_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS log_analyses (
    id BIGSERIAL PRIMARY KEY,
    log_file_id BIGINT NOT NULL REFERENCES log_files(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    total_lines BIGINT DEFAULT 0,
    total_errors INT DEFAULT 0,
    total_warnings INT DEFAULT 0,
    critical_errors INT DEFAULT 0,
    unique_patterns INT DEFAULT 0,
    duration_ms BIGINT DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    summary TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS parsed_errors (
    id BIGSERIAL PRIMARY KEY,
    analysis_id BIGINT NOT NULL REFERENCES log_analyses(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL,
    error_timestamp VARCHAR(100),
    file_name VARCHAR(255),
    line_number INT,
    stack_trace TEXT,
    service_name VARCHAR(100),
    occurrence_count INT DEFAULT 1,
    first_occurrence VARCHAR(100),
    last_occurrence VARCHAR(100),
    fingerprint VARCHAR(120),
    is_primary BOOLEAN DEFAULT FALSE,
    supporting_evidence TEXT,
    raw_snippet TEXT
);

CREATE TABLE IF NOT EXISTS root_cause_reports (
    id BIGSERIAL PRIMARY KEY,
    parsed_error_id BIGINT NOT NULL UNIQUE REFERENCES parsed_errors(id) ON DELETE CASCADE,
    analysis_id BIGINT NOT NULL REFERENCES log_analyses(id) ON DELETE CASCADE,
    what_went_wrong TEXT NOT NULL,
    why_it_happened TEXT NOT NULL,
    affected_component VARCHAR(255) NOT NULL,
    impact_assessment TEXT NOT NULL,
    primary_or_secondary VARCHAR(50) NOT NULL,
    supporting_evidence TEXT NOT NULL,
    immediate_fix TEXT NOT NULL,
    step_by_step_debugging TEXT NOT NULL,
    suggested_code_change TEXT,
    troubleshooting_commands TEXT,
    long_term_prevention TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    log_file_id BIGINT NOT NULL REFERENCES log_files(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sender_role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    references_json TEXT,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_log_files_user ON log_files(user_id);
CREATE INDEX IF NOT EXISTS idx_log_analyses_file ON log_analyses(log_file_id);
CREATE INDEX IF NOT EXISTS idx_parsed_errors_analysis ON parsed_errors(analysis_id);
CREATE INDEX IF NOT EXISTS idx_parsed_errors_severity ON parsed_errors(severity);
CREATE INDEX IF NOT EXISTS idx_parsed_errors_category ON parsed_errors(category);
