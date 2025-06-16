package est.secretary.controller;

import java.util.Collections;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import est.secretary.dto.AiActionRequest;
import est.secretary.service.AiActionService;
import lombok.RequiredArgsConstructor;

// AiActionController.java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiActionController {

	private final AiActionService aiActionService;

	@PostMapping("/action")
	public ResponseEntity<?> handleAiAction(@RequestBody AiActionRequest request) {
		String responseMessage = aiActionService.processAction(request.getActionQuery(), request.getHistory());
		return ResponseEntity.ok(Collections.singletonMap("message", responseMessage));
	}
}
