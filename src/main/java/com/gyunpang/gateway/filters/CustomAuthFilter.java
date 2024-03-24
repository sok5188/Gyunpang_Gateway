package com.gyunpang.gateway.filters;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class CustomAuthFilter extends AbstractGatewayFilterFactory<CustomAuthFilter.Config> {

	public CustomAuthFilter(){
		super(Config.class);
		log.debug("[Filter] CustomAuthFilter invoked");
	}

	@Override
	public GatewayFilter apply(Config config) {
		return ((exchange, chain) -> {
			log.debug("[Filter] got request");
			ServerHttpRequest request = exchange.getRequest();

			// Request Header 에 token 이 존재하지 않을 때
			if(!request.getHeaders().containsKey("token")){
				return unAuthrizeHandler(exchange); // 401 Error
			}

			log.debug("[Filter] request is valid");

			return chain.filter(exchange); // 토큰이 일치할 때

		});
	}

	private Mono<Void> unAuthrizeHandler(ServerWebExchange exchange) {
		ServerHttpResponse response = exchange.getResponse();

		response.setStatusCode(HttpStatus.UNAUTHORIZED);
		log.debug("[Filter] unAuthorized !!");
		return response.setComplete();
	}

	public static class Config{

	}
}
