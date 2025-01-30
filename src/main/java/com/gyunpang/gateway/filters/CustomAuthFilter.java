package com.gyunpang.gateway.filters;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import com.gyunpang.gateway.utils.GatewayConstant;
import com.gyunpang.gateway.utils.JwtUtil;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class CustomAuthFilter extends AbstractGatewayFilterFactory<CustomAuthFilter.Config> {

	private final JwtUtil jwtUtil;

	public CustomAuthFilter(JwtUtil jwtUtil) {
		super(Config.class);
		this.jwtUtil = jwtUtil;
	}

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			String authorizationHeader = exchange.getRequest().getHeaders().getFirst(config.headerName);
			if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(config.granted + " ")) {
				String token = authorizationHeader.substring(7); // Bearer
				try {
					Jws<Claims> claims = jwtUtil.getClaims(token);
					Claims payload = claims.getPayload();

					String authority = String.valueOf(
						payload.getOrDefault(GatewayConstant.HEADER_AUTHORITY, ""));
					String username = String.valueOf(payload.getOrDefault(GatewayConstant.HEADER_USERNAME, ""));
					log.info("got authority : {} username: {}", authority, username);
					ServerHttpRequest request = exchange.getRequest();

					request.mutate().header(GatewayConstant.HEADER_AUTHORITY, authority);
					request.mutate().header(GatewayConstant.HEADER_USERNAME, username);
					request.mutate().headers(httpHeaders -> httpHeaders.remove("Authorization"));
					return chain.filter(exchange);
				} catch (Exception e) {
					log.error("Token validation error: {}, class : {}", e.getMessage(), e.getClass());
				}
			}
			return unauthorizedResponse(exchange); // Token is not valid, respond with unauthorized
		};
	}

	private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
		log.info("Custom Auth Filter will make 401 response");
		exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
		return exchange.getResponse().setComplete();
	}

	@Getter
	@Setter
	public static class Config {
		private String headerName; // Authorization
		private String granted; // Bearer
	}
}
