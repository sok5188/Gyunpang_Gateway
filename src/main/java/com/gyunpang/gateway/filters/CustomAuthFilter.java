package com.gyunpang.gateway.filters;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import com.gyunpang.gateway.utils.CommonCode;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class CustomAuthFilter extends AbstractGatewayFilterFactory<CustomAuthFilter.Config> {

	private final TokenProvider tokenProvider;
	public CustomAuthFilter(TokenProvider tokenProvider){
		super(Config.class);
		this.tokenProvider = tokenProvider;
	}
	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			String authorizationHeader = exchange.getRequest().getHeaders().getFirst(config.headerName);
			if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(config.granted+" ")) {
				String token = authorizationHeader.substring(7); // Bearer
				try {
					String username = tokenProvider.validateTokenAndGetUsername(token);
					ServerHttpRequest request = exchange.getRequest();
					request.mutate().header(CommonCode.HEADER_USERNAME.getContext(),username);
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
		exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
		return exchange.getResponse().setComplete();
	}

	@Getter
	@Setter
	public static class Config{
		private String headerName; // Authorization
		private String granted; // Bearer
	}
}
