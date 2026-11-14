package com.loganalyzer.controller;

import com.loganalyzer.dto.ApiResponse;
import com.loganalyzer.dto.DashboardSummaryDto;
import com.loganalyzer.entity.User;
import com.loganalyzer.service.AuthService;
import com.loganalyzer.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Endpoints for aggregate system statistics and error metrics")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuthService authService;

    public DashboardController(DashboardService dashboardService, AuthService authService) {
        this.dashboardService = dashboardService;
        this.authService = authService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get high-level dashboard metrics, distributions, and trends")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getDashboardSummary() {
        User user = authService.getCurrentAuthenticatedUser();
        DashboardSummaryDto summary = dashboardService.getSummary(user);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}
