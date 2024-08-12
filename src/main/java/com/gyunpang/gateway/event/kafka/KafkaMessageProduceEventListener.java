package com.gyunpang.gateway.event.kafka;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.gyunpang.gateway.service.KafkaService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KafkaMessageProduceEventListener {
	private final KafkaService kafkaService;
	@Async
	@EventListener
	public void onKafkaMessageHandler(KafkaMessageProduceEvent event){
		kafkaService.produceMessageToTopic(event.getTopic(), event.getContext());
	}
}
