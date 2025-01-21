package com.gyunpang.gateway.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.gyunpang.gateway.dto.AuthDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebClientService {
	private final WebClient webClient;

	public String sendSignInRequest(AuthDto.SignInReq req) {
		return getWebClient()
			.put()
			.uri("/open/signIn")
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.APPLICATION_JSON)
			.bodyValue(req)
			.retrieve()
			.bodyToMono(String.class)
			.onErrorResume(Mono::error)
			.block();
	}

	private WebClient getWebClient() {
		String baseUrl = UriComponentsBuilder.newInstance().scheme("https")
			.host("localhost:8080")
			.build().toString();
		return webClient.mutate().baseUrl(baseUrl).build();
	}
}
