package com.network.projet.ussd.controller.admin;

import com.network.projet.ussd.dto.request.LoginRequest;
import com.network.projet.ussd.dto.request.RegisterRequest;
import com.network.projet.ussd.dto.response.AuthResponse;
import com.network.projet.ussd.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminService adminService;

    @PostMapping("/login")
    public Mono<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        return adminService.login(request);
    }

    @PostMapping("/register")
    public Mono<AuthResponse> register(@RequestBody RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());
        return adminService.register(request);
    }
}
