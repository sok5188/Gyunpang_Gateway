package com.gyunpang.gateway.contorller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class tmpController {

	@Value("${CONTAINER_COLOR}")
	private String colorInfo;
	@RequestMapping("/health")
	public ResponseEntity<String> healthCheck(){
		return ResponseEntity.ok("alive");
	}

	@RequestMapping("/color")
	public ResponseEntity<String> colorCheck(){
		return ResponseEntity.ok(colorInfo);
	}
}
