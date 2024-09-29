package com.gyunpang.gateway.contorller;

import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequiredArgsConstructor
public class tmpController {

	private final Environment environment;

	@RequestMapping("/healthCheck")
	public ResponseEntity<String> healthCheck() {
		log.info("got health check");

		return ResponseEntity.ok("gateway alive");
	}

	@RequestMapping("/color")
	public ResponseEntity<String> colorCheck() {
		log.info("got color check");
		if (environment.containsProperty("CONTAINER_COLOR")) {
			String customEnv = environment.getProperty("CONTAINER_COLOR");
			return ResponseEntity.ok(customEnv);
		} else
			return ResponseEntity.ok("NoCOLOR");
	}
}
