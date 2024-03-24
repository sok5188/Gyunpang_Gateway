package com.gyunpang.gateway.contorller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class tmpController {
	@RequestMapping("/health")
	public ResponseEntity<String> healthCheck(){
		return ResponseEntity.ok("alive");
	}

	@RequestMapping("/color")
	public ResponseEntity<String> colorCheck(){
		String color = System.getProperty("color");
		return ResponseEntity.ok(color);
	}
}
