package com.gyunpang.gateway.filters;

import java.io.ByteArrayOutputStream;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyunpang.gateway.event.kafka.KafkaMessageProduceEvent;
import com.gyunpang.gateway.event.kafka.KafkaMessageProduceEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingGlobalFilter implements GlobalFilter, Ordered {
	private final KafkaMessageProduceEventPublisher kafkaMessageProduceEventPublisher;
	@Value(value = "${kafka.topic.log}")
	private String logTopic;
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		Map<String, Object> requestMap = new HashMap<>();
		Map<String, Object> responseMap = new HashMap<>();
		ServerHttpRequest request = getDecoratedRequest(exchange, requestMap);
		ServerHttpResponse response = getDecoratedResponse(exchange, responseMap);

		return chain.filter(exchange.mutate().request(request).response(response).build())
			.doOnError(err -> {
				log.error("err !"+err.getMessage());
				kafkaMessageProduceEventPublisher.publishKafkaMessage(KafkaMessageProduceEvent.builder().topic(logTopic).context("["+exchange.getRequest().getId()+"]"+" ERROR message : "+err.getMessage()).build());
			})
			.then(Mono.fromRunnable(() -> {
				StringBuilder sb = new StringBuilder();

				sb.append("[").append(exchange.getRequest().getId()).append("] request info : { ");
				sendLogMessage(requestMap, sb);

				sb = new StringBuilder();
				sb.append("[").append(exchange.getRequest().getId()).append("] response status : ").append(response.getStatusCode()).append(" response info : { ");
				sendLogMessage(responseMap, sb);

			}));
	}

	private void sendLogMessage(Map<String, Object> responseMap, StringBuilder sb) {
		for (String key : responseMap.keySet()) {
			sb.append(key).append(" : ").append(responseMap.get(key)).append(" ");
		}
		sb.append("}");
		log.info(sb.toString());
		kafkaMessageProduceEventPublisher.publishKafkaMessage(KafkaMessageProduceEvent.builder().topic(logTopic).context(
			sb.toString()).build());
	}

	@Override
	public int getOrder() {
		return -1;
	}



	private ServerHttpRequest getDecoratedRequest(ServerWebExchange exchange, Map<String, Object> requestMap) {

		ServerHttpRequest request = exchange.getRequest();
		requestMap.put("method", request.getMethod());
		requestMap.put("uri",request.getURI());

		if(!request.getQueryParams().isEmpty())
			requestMap.put("query", request.getQueryParams());

		Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

		if (route != null) {
			requestMap.put("routeId", route.getId());
			requestMap.put("routeUri", route.getUri());
		}

		return new ServerHttpRequestDecorator(request) {
			@Override
			public Flux<DataBuffer> getBody() {
				return super.getBody().publishOn(Schedulers.boundedElastic()).doOnNext(dataBuffer -> {
					try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

						Channels.newChannel(byteArrayOutputStream).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
						String requestBody = removeWhiteSpacesFromJson(byteArrayOutputStream.toString(StandardCharsets.UTF_8));
						requestMap.put("body", requestBody);

					} catch (Exception e) {
						log.error(e.getMessage());
					}
				});
			}
		};
	}

	private ServerHttpResponseDecorator getDecoratedResponse(ServerWebExchange exchange,
		Map<String, Object> responseMap) {

		ServerHttpResponse response = exchange.getResponse();
		DataBufferFactory dataBufferFactory = response.bufferFactory();

		return new ServerHttpResponseDecorator(response) {
			@Override
			public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {

				if (body instanceof Flux) {
					Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;

					return super.writeWith(fluxBody.buffer().map(dataBuffers -> {

						DefaultDataBuffer joinedBuffers = new DefaultDataBufferFactory().join(dataBuffers);
						byte[] content = new byte[joinedBuffers.readableByteCount()];
						joinedBuffers.read(content);

						responseMap.put("body", new String(content, StandardCharsets.UTF_8));

						return dataBufferFactory.wrap(content);
					})).onErrorResume(err -> {
						log.error(err.getMessage());
						return Mono.empty();
					});
				}else{
					log.warn(exchange.getRequest().getId()+ "'s response has no body");
				}
				return super.writeWith(body);
			}
		};
	}

	private String removeWhiteSpacesFromJson(String json) {
		ObjectMapper om = new ObjectMapper();
		try {
			JsonNode jsonNode = om.readTree(json);
			return om.writeValueAsString(jsonNode);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

}
