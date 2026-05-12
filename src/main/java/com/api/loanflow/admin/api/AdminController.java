package com.api.loanflow.admin.api;

import com.api.loanflow.admin.api.dto.AdminDashboardResponse;
import com.api.loanflow.admin.application.AdminDashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
	private final AdminDashboardService adminDashboardService;

	public AdminController(AdminDashboardService adminDashboardService) {
		this.adminDashboardService = adminDashboardService;
	}

	@GetMapping("/dashboard")
	public AdminDashboardResponse dashboard() {
		return adminDashboardService.dashboard();
	}
}
