package com.gyunpang.gateway.utils;

import lombok.Getter;

@Getter
public enum CommonCode {
	HEADER_USERNAME("username"),
	HEADER_AUTHORITY("authority");
	private String context;

	CommonCode(String context) {
		this.context = context;
	}
}
