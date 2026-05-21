package com.sportvenue.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Health check & Hello World controller.
 * Dùng để xác nhận backend đang chạy bình thường.
 */
@Tag(name = "Health", description = "Health check & system status")
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @Operation(summary = "Hello World", description = "Endpoint xác nhận backend đang hoạt động")
    @GetMapping("/hello")
    public ResponseEntity<Map<String, Object>> hello() {
        return ResponseEntity.ok(Map.of(
                "message", "🏟️ Sport Venue API is running!",
                "status", "OK",
                "timestamp", LocalDateTime.now().toString(),
                "version", "0.0.1-SNAPSHOT"
        ));
    }

    @Operation(summary = "Ping", description = "Simple ping endpoint")
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
