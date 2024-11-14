package com.frc.codex.controllers;

import java.util.Objects;

import com.frc.codex.FilingIndexProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public abstract class BaseController {

	protected final FilingIndexProperties properties;

	protected BaseController(
			FilingIndexProperties properties
	) {
		this.properties = properties;
	}

	protected boolean authenticateAdmin(HttpServletRequest request) {
		if (!properties.adminEnabled()) {
			// Admin disabled
			return false;
		}
		if (Objects.equals(properties.adminKey(), "")) {
			// Empty adminKey means no authentication required
			return true;
		}
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (
						cookie.getName().equals(properties.adminCookieName()) &&
						cookie.getValue().equals(properties.adminKey())
				) {
					return true;
				}
			}
		}
		return false;
	}
}
