package com.gyunpang.gateway.contorller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.gyunpang.gateway.dto.AuthDto;
import com.gyunpang.gateway.service.AuthService;
import com.gyunpang.gateway.utils.GatewayConstant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/gateway")
@CrossOrigin(origins = {"https://localhost:3000", "https://sirong.shop"}
	, methods = {RequestMethod.GET, RequestMethod.PUT, RequestMethod.POST, RequestMethod.OPTIONS}
	, exposedHeaders = {GatewayConstant.ACCESS_TOKEN, HttpHeaders.SET_COOKIE}
	, allowCredentials = "true")
public class AuthController {

	private final AuthService authService;

	@PostMapping("/signin")
	public ResponseEntity<AuthDto.SignInRes> signIn(@RequestHeader Map<String, String> headers,
		@RequestBody AuthDto.SignInReq req) {
		log.info("try sign in");
		AuthDto.SignInRes res = AuthDto.SignInRes.builder().build();
		if (headers.containsKey(GatewayConstant.ACCESS_TOKEN)) {
			res = authService.trySignInWithToken(headers.get(GatewayConstant.ACCESS_TOKEN));
		}
		if (Optional.ofNullable(res.getAccessToken()).isEmpty() && headers.containsKey(
			GatewayConstant.REFRESH_TOKEN)) {
			res = authService.trySignInWithToken(headers.get(GatewayConstant.REFRESH_TOKEN));
		}
		if (Optional.ofNullable(res.getAccessToken()).isEmpty() && authService.trySignInWithPassword(req)) {
			res = authService.getAuthTokens(req.getUsername());
		}

		if (Optional.ofNullable(res.getAccessToken()).isPresent()) {
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.set(GatewayConstant.ACCESS_TOKEN, res.getAccessToken());

			ResponseCookie cookie = ResponseCookie.from(GatewayConstant.REFRESH_TOKEN, res.getRefreshToken())
				.httpOnly(true)
				.secure(true)
				.sameSite("None")
				.maxAge(10000)
				.build();
			httpHeaders.set(HttpHeaders.SET_COOKIE, cookie.toString());
			log.info("success");
			return ResponseEntity
				.ok()
				.headers(httpHeaders)
				.body(res);
		} else {
			log.info("fail");
			return ResponseEntity.badRequest().body(res);
		}
	}

	@PostMapping("/post")
	public ResponseEntity<String> postTest(@RequestBody AuthDto.SignInReq req) {
		return ResponseEntity.ok("Got Body " + req.getUsername());
	}
}
