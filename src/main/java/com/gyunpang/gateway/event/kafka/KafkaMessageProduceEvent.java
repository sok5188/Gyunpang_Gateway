package com.gyunpang.gateway.event.kafka;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KafkaMessageProduceEvent {
	private String topic;
	private String context;
}
