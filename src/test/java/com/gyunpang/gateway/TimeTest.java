package com.gyunpang.gateway;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.junit.jupiter.api.Test;

public class TimeTest {

	@Test
	void LocalToDate() {
		System.out.println(LocalDateTime.now());
		System.out.println(LocalDateTime.now().toLocalDate());
		System.out.println(Date.from(
			LocalDateTime.now().toInstant(ZoneOffset.ofHours(9))
		));
	}
}
