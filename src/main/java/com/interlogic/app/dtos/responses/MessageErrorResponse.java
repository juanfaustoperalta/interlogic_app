package com.interlogic.app.dtos.responses;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class MessageErrorResponse {

	private String timesStamp;
	private Integer status;
	private Map<String, List<String>> errorDetails;
	private String path;

	public MessageErrorResponse(Integer status, Map<String, List<String>> errorDetails, String path) {
		this.timesStamp = LocalDateTime.now().toString();
		this.status = status;
		this.errorDetails = errorDetails;
		this.path = path;
	}

	public String getTimesStamp() {
		return timesStamp;
	}

	public void setTimesStamp(String timesStamp) {
		this.timesStamp = timesStamp;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}


	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Map<String, List<String>> getErrorDetails() {
		return errorDetails;
	}

	public void setErrorDetails(Map<String, List<String>> errorDetails) {
		this.errorDetails = errorDetails;
	}
}