package com.network.projet.ussd.service.admin;

import com.network.projet.ussd.domain.model.Admin;
import com.network.projet.ussd.dto.request.LoginRequest;
import com.network.projet.ussd.dto.request.RegisterRequest;
import com.network.projet.ussd.dto.response.AuthResponse;
import com.network.projet.ussd.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;

    public Mono<AuthResponse> register(RegisterRequest request) {
        return adminRepository.findByEmail(request.getEmail())
                .flatMap(existing -> Mono.<Admin>error(
                        new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cet email est déjà utilisé")))
                .switchIfEmpty(Mono.defer(() -> {
                    Admin admin = Admin.builder()
                            .username(request.getName())
                            .email(request.getEmail())
                            .password(hashPassword(request.getPassword()))
                            .role("ADMIN")
                            .createdAt(LocalDateTime.now())
                            .build();
                    return adminRepository.save(admin);
                }))
                .map(this::toAuthResponse);
    }

    public Mono<AuthResponse> login(LoginRequest request) {
        return adminRepository.findByEmail(request.getEmail())
                .filter(admin -> admin.getPassword().equals(hashPassword(request.getPassword())))
                .map(this::toAuthResponse)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect")));
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error hashing password", e);
            return password; // Fallback unsafe
        }
    }

    private AuthResponse toAuthResponse(Admin admin) {
        return AuthResponse.builder()
                .id(admin.getId().toString())
                .name(admin.getUsername())
                .email(admin.getEmail())
                .token("jwt-session-" + admin.getId() + "-" + System.currentTimeMillis())
                .build();
    }
}
