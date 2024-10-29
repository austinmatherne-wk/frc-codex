package com.frc.codex.model;

public class SurveyRequest {
	private Integer searchUtilityRating;
	private Integer searchSpeedRating;
	private Integer viewerSpeedRating;

	public Integer getSearchUtilityRating() {
		return searchUtilityRating;
	}

	public Integer getSearchSpeedRating() {
		return searchSpeedRating;
	}

	public Integer getViewerSpeedRating() {
		return viewerSpeedRating;
	}

	public void setSearchUtilityRating(Integer searchUtilityRating) {
		this.searchUtilityRating = searchUtilityRating;
	}

	public void setSearchSpeedRating(Integer searchSpeedRating) {
		this.searchSpeedRating = searchSpeedRating;
	}

	public void setViewerSpeedRating(Integer viewerSpeedRating) {
		this.viewerSpeedRating = viewerSpeedRating;
	}
}
