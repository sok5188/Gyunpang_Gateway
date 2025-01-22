package com.gyunpang.gateway.contorller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import com.gyunpang.gateway.dto.AuthDto;
import com.gyunpang.gateway.service.AuthService;
import com.gyunpang.gateway.utils.GatewayConstant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/gateway")
public class AuthController {

	private final AuthService authService;

	@PutMapping("/signIn")
	public ResponseEntity<AuthDto.SignInRes> signIn(@RequestHeader Map<String, String> headers,
		@RequestBody AuthDto.SignInReq req) {
		AuthDto.SignInRes res = AuthDto.SignInRes.builder().build();
		if (headers.containsKey(GatewayConstant.ACCESS_HEADER)) {
			res = authService.trySignInWithToken(headers.get(GatewayConstant.ACCESS_HEADER));
		}
		if (Optional.ofNullable(res.getAccessToken()).isEmpty() && headers.containsKey(
			GatewayConstant.REFRESH_HEADER)) {
			res = authService.trySignInWithToken(headers.get(GatewayConstant.REFRESH_HEADER));
		}
		if (Optional.ofNullable(res.getAccessToken()).isEmpty() && authService.trySignInWithPassword(req)) {
			res = authService.getAuthTokens(req.getUsername());
		}

		if (Optional.ofNullable(res.getAccessToken()).isPresent()) {
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.set(GatewayConstant.ACCESS_HEADER, res.getAccessToken());
			httpHeaders.set(GatewayConstant.REFRESH_HEADER, res.getRefreshToken());
			return ResponseEntity
				.ok()
				.headers(httpHeaders)
				.body(res);
		} else {
			return ResponseEntity.badRequest().body(res);
		}
	}
}
