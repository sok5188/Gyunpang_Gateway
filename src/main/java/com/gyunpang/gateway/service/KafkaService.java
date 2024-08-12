package com.gyunpang.gateway.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaService {
	private final KafkaTemplate<String, String> kafkaTemplate;
	public void produceMessageToTopic(String topic, String message){
		CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, message);
		future.whenComplete((result,ex)->{
			if(ex==null){
				log.debug("[producer] sent message=[{}] with offset=[{}]", message, result.getRecordMetadata().offset());
			}
			else log.warn("[producer] unable to send message.. {}",ex.getMessage());
		});
	}
}
