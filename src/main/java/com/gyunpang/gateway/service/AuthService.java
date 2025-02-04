package com.gyunpang.gateway.service;

import org.springframework.stereotype.Service;

import com.gyunpang.gateway.dto.AuthDto;
import com.gyunpang.gateway.utils.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
	private final JwtUtil jwtUtil;
	private final WebClientService webClientService;

	public AuthDto.SignInRes trySignInWithToken(String token) {
		return jwtUtil.tryRefreshToken(token);
	}

	public Integer trySignInWithPassword(AuthDto.SignInReq req) {
		if (req.getUsername().isEmpty() || req.getPassword().isEmpty())
			return -1;

		return webClientService.sendSignInRequest(req).block();
	}

	public AuthDto.SignInRes getAuthTokens(String username, Integer authority) {
		return jwtUtil.generateToken(username, authority);
	}

	public boolean validateToken(String token) {
		return jwtUtil.validateToken(token);
	}
}
