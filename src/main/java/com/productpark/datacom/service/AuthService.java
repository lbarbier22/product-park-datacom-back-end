package com.productpark.datacom.service;

import com.productpark.datacom.dto.request.LoginRequest;
import com.productpark.datacom.dto.response.AuthResponse;
import com.productpark.datacom.model.entity.User;
import com.productpark.datacom.repository.UserRepository;
import com.productpark.datacom.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> new BadCredentialsException("Identifiants incorrects"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Identifiants incorrects");
        }

        String token = jwtTokenProvider.generateToken(user.getLogin(), user.getRole().name());

        return new AuthResponse(
                token,
                user.getLogin(),
                user.getFirstname(),
                user.getLastname(),
                user.getRole()
        );
    }

}
