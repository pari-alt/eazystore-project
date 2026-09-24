package com.eazybytes.eazystore.controller;

import com.eazybytes.eazystore.service.AIShoppingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIShoppingService aiShoppingService;

    @PostMapping("/chat")
    public Map<String, String> chat(
            @RequestBody Map<String, String> request) {

        String message = request.get("message");

        if (message == null || message.isBlank()) {
            return Map.of(
                    "response",
                    "Please tell me what product you are looking for."
            );
        }

        String response =
                aiShoppingService.getShoppingRecommendation(message);

        return Map.of("response", response);
    }
}