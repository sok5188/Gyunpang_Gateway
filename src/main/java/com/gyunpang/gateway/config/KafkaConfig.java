package com.gyunpang.gateway.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
	@Value(value = "${spring.kafka.bootstrap-servers}")
	private String bootStrapAddress;
	@Value(value = "${kafka.topic.log}")
	private String logTopic;

	@Bean
	public KafkaAdmin kafkaAdmin(){
		Map<String,Object> configs=new HashMap<>();
		configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapAddress);
		return new KafkaAdmin(configs);
	}
	@Bean
	public NewTopic topic1(){
		return TopicBuilder.name(logTopic)
			.partitions(10)
			.replicas(1)
			.build();
	}

	@Bean
	public ProducerFactory<String,String> producerFactory(){
		Map<String,Object> props=new HashMap<>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapAddress);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		return new DefaultKafkaProducerFactory<>(props);
	}
	@Bean
	public KafkaTemplate<String,String> kafkaTemplate(){
		return new KafkaTemplate<String,String>(producerFactory());
	}

}
