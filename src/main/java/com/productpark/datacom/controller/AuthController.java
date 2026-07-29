package com.productpark.datacom.controller;

import com.productpark.datacom.dto.request.LoginRequest;
import com.productpark.datacom.dto.response.AuthResponse;
import com.productpark.datacom.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

}
