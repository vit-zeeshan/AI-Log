package com.loganalyzer.entity;

public enum ErrorCategory {
    APPLICATION_ERRORS("Application Errors"),
    JAVA_EXCEPTIONS("Java Exceptions"),
    DATABASE_ERRORS("Database Errors"),
    API_ERRORS("API Errors"),
    NETWORK_ERRORS("Network Errors"),
    AUTHENTICATION_ERRORS("Authentication Errors"),
    MEMORY_ISSUES("Memory Issues"),
    CPU_PERFORMANCE_ISSUES("CPU Performance Issues"),
    TIMEOUT_ERRORS("Timeout Errors"),
    CONFIGURATION_ERRORS("Configuration Errors"),
    INFRASTRUCTURE_ERRORS("Infrastructure Errors"),
    DEPLOYMENT_ERRORS("Deployment Errors"),
    DEPENDENCY_FAILURES("Dependency Failures"),
    SECURITY_RELATED_EVENTS("Security-related Events");

    private final String displayName;

    ErrorCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
