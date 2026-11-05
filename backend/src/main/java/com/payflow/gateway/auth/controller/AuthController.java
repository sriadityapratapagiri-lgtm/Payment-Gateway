package com.payflow.gateway.auth.controller;

import com.payflow.gateway.auth.dto.AuthResponse;
import com.payflow.gateway.auth.dto.LoginRequest;
import com.payflow.gateway.auth.dto.RegisterRequest;
import com.payflow.gateway.auth.service.AuthService;
import com.payflow.gateway.common.dto.ApiResponse;
import com.payflow.gateway.common.security.MerchantPrincipal;
import com.payflow.gateway.merchant.entity.Merchant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Merchant registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(@AuthenticationPrincipal MerchantPrincipal principal) {
        Merchant merchant = authService.getMe(principal.getMerchantId());
        Map<String, Object> data = Map.of(
            "id", merchant.getId(),
            "businessName", merchant.getBusinessName(),
            "email", merchant.getEmail(),
            "status", merchant.getStatus(),
            "createdAt", merchant.getCreatedAt()
        );
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}

