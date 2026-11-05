package com.payflow.gateway.auth.service;

import com.payflow.gateway.auth.dto.AuthResponse;
import com.payflow.gateway.auth.dto.LoginRequest;
import com.payflow.gateway.auth.dto.RegisterRequest;
import com.payflow.gateway.common.exception.BusinessRuleException;
import com.payflow.gateway.common.security.JwtTokenProvider;
import com.payflow.gateway.merchant.entity.Merchant;
import com.payflow.gateway.merchant.repository.MerchantRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(MerchantRepository merchantRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       AuthenticationManager authenticationManager) {
        this.merchantRepository = merchantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (merchantRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Email already registered", "EMAIL_ALREADY_EXISTS");
        }

        Merchant merchant = new Merchant(
            request.businessName(),
            request.email(),
            passwordEncoder.encode(request.password())
        );
        merchant = merchantRepository.save(merchant);

        String token = jwtTokenProvider.generateToken(merchant.getId(), merchant.getEmail());
        return new AuthResponse(token, merchant.getId(), merchant.getEmail(),
            merchant.getBusinessName(), merchant.getCreatedAt());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        Merchant merchant = merchantRepository.findByEmail(request.email())
            .orElseThrow(() -> new BusinessRuleException("Merchant not found", "MERCHANT_NOT_FOUND"));

        String token = jwtTokenProvider.generateToken(merchant.getId(), merchant.getEmail());
        return new AuthResponse(token, merchant.getId(), merchant.getEmail(),
            merchant.getBusinessName(), merchant.getCreatedAt());
    }

    public Merchant getMe(java.util.UUID merchantId) {
        return merchantRepository.findById(merchantId)
            .orElseThrow(() -> new BusinessRuleException("Merchant not found", "MERCHANT_NOT_FOUND"));
    }
}

