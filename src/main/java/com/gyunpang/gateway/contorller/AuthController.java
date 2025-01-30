package com.gyunpang.gateway.contorller;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.server.ServerWebExchange;

import com.gyunpang.gateway.dto.AuthDto;
import com.gyunpang.gateway.service.AuthService;
import com.gyunpang.gateway.utils.GatewayConstant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/gateway")
@CrossOrigin(origins = {"http://localhost:3000", "https://sirong.shop"}
	, methods = {RequestMethod.GET, RequestMethod.PUT, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.OPTIONS}
	, exposedHeaders = {GatewayConstant.ACCESS_TOKEN, HttpHeaders.SET_COOKIE}
	, allowCredentials = "true")
public class AuthController {

	private final AuthService authService;

	@PostMapping("/signin")
	public Mono<ResponseEntity<AuthDto.SignInRes>> signIn(@RequestHeader Map<String, String> headers,
		@RequestBody AuthDto.SignInReq req, ServerWebExchange exchange) {
		log.info("try sign in");
		AuthDto.SignInRes res = AuthDto.SignInRes.builder().build();
		MultiValueMap<String, HttpCookie> cookies = exchange.getRequest().getCookies();

		//Try With RefreshToken
		if (cookies.containsKey(GatewayConstant.REFRESH_TOKEN)) {
			log.info("refresh is present");
			res = authService.trySignInWithToken(
				Objects.requireNonNull(cookies.getFirst(GatewayConstant.REFRESH_TOKEN)).getValue());
			Mono<ResponseEntity<AuthDto.SignInRes>> monoRes = tryReturnAuthResponse(
				res);
			if (monoRes != null)
				return monoRes;
		}

		//Try With auth info
		Integer authType = authService.trySignInWithPassword(req);
		if (authType != -1) {
			log.info("try auth info");
			Mono<ResponseEntity<AuthDto.SignInRes>> monoRes = tryReturnAuthResponse(
				authService.getAuthTokens(req.getUsername(), authType));
			if (monoRes != null)
				return monoRes;
		}

		log.info("try auth info TT");
		return Mono.just(ResponseEntity.badRequest().body(res));
	}

	private static Mono<ResponseEntity<AuthDto.SignInRes>> tryReturnAuthResponse(AuthDto.SignInRes res) {
		if (Optional.ofNullable(res.getAccessToken()).isPresent()) {
			HttpHeaders httpHeaders = setAuthorized(res);
			return Mono.just(ResponseEntity
				.ok()
				.headers(httpHeaders)
				.body(res));
		}
		return null;
	}

	private static HttpHeaders setAuthorized(AuthDto.SignInRes res) {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set(GatewayConstant.ACCESS_TOKEN, res.getAccessToken());

		ResponseCookie cookie = ResponseCookie.from(GatewayConstant.REFRESH_TOKEN, res.getRefreshToken())
			.httpOnly(true)
			.secure(true)
			.sameSite("None")
			.maxAge(Duration.ofDays(10))
			.build();
		httpHeaders.set(HttpHeaders.SET_COOKIE, cookie.toString());
		return httpHeaders;
	}

	@DeleteMapping("/signout")
	public ResponseEntity<String> signOut(ServerWebExchange exchange) {
		log.info("sign out");
		ResponseCookie cookie = ResponseCookie.from(GatewayConstant.REFRESH_TOKEN, "")
			.maxAge(0)
			.httpOnly(true)
			.secure(true)
			.sameSite("None")
			.build();

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookie.toString())
			.body("로그아웃 완료");
	}

	@PutMapping("/refresh")
	public ResponseEntity<AuthDto.SignInRes> refreshToken(ServerWebExchange exchange) {
		log.info("try refresh");
		AuthDto.SignInRes res = AuthDto.SignInRes.builder().build();
		MultiValueMap<String, HttpCookie> cookies = exchange.getRequest().getCookies();

		if (Optional.ofNullable(res.getAccessToken()).isEmpty() && cookies.containsKey(GatewayConstant.REFRESH_TOKEN)) {
			res = authService.trySignInWithToken(
				Objects.requireNonNull(cookies.getFirst(GatewayConstant.REFRESH_TOKEN)).getValue());
		}

		if (Optional.ofNullable(res.getAccessToken()).isPresent()) {
			HttpHeaders httpHeaders = setAuthorized(res);
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

	@GetMapping("/cookie")
	public ResponseEntity<String> cookieTest(ServerWebExchange exchange) {
		MultiValueMap<String, HttpCookie> cookies = exchange.getRequest().getCookies();
		StringBuilder sb = new StringBuilder();
		for (String s : cookies.keySet()) {
			sb.append("key : ").append(s).append(" / val : ")
				.append(cookies.get(s))
				.append("\n");
		}
		return ResponseEntity.ok(sb.toString());
	}
}
