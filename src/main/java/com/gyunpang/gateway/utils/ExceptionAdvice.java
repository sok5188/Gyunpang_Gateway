package com.gyunpang.gateway.utils;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ExceptionAdvice {
	@ExceptionHandler(WebClientResponseException.class)
	public ResponseEntity<String> exceptionHandler(WebClientResponseException e) {
		return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
	}
}
