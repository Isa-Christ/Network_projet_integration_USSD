package com.network.projet.ussd.controller;

import com.network.projet.ussd.dto.request.UssdRequest;
import com.network.projet.ussd.dto.response.UssdResponse;
import com.network.projet.ussd.service.core.UssdGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/ussd")
@RequiredArgsConstructor
public class UssdController {
    
    private final UssdGatewayService gatewayService;
    
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<UssdResponse> handleUssd(@RequestBody UssdRequest request) {
        return gatewayService.handleUssdRequest(request);
    }
    
    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("USSD Gateway is running ✅");
    }
}