package com.eqochat.controller;

import com.eqochat.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {
    
    @GetMapping
    public Mono<ApiResponse<String>> health() {
        return Mono.just(ApiResponse.success("EqoChat Backend is running!"));
    }
}