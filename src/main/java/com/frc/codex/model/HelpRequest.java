package com.frc.codex.model;

public class HelpRequest {
	private String emailAddress;
	private String message;
	private String name;
	private String referer;

	public String getEmailAddress() {
		return emailAddress;
	}

	public String getMessage() {
		return message;
	}

	public String getName() {
		return name;
	}

	public String getReferer() {
		return referer;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setReferer(String referer) {
		this.referer = referer;
	}

	public String toString() {
		return "HelpRequest [name=" + name + ", emailAddress=" + emailAddress + ", referer=" + referer + ", message=" + message + "]";
	}
}
