package com.gyunpang.gateway.event.kafka;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KafkaMessageProduceEventPublisher {
	private final ApplicationEventPublisher applicationEventPublisher;

	public void publishKafkaMessage(KafkaMessageProduceEvent event){
		applicationEventPublisher.publishEvent(event);
	}
}
