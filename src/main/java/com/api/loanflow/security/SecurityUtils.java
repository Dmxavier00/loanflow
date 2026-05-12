package com.api.loanflow.security;

import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
	private SecurityUtils() {
	}

	public static String emailAutenticado() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication == null ? null : authentication.getName();
	}
}
