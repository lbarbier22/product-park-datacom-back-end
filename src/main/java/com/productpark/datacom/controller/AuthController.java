package com.productpark.datacom.controller;

import com.productpark.datacom.dto.request.LoginRequest;
import com.productpark.datacom.dto.response.AuthResponse;
import com.productpark.datacom.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Endpoints pour l'authentification et la gestion des sessions utilisateur")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Authentification de l'utilisateur",
            description = "Permet à un utilisateur de se connecter avec ses identifiants et de récupérer un jeton d'accès."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentification réussie, jeton retourné"),
            @ApiResponse(responseCode = "400", description = "Données de la requête invalides", content = @Content),
            @ApiResponse(responseCode = "401", description = "Identifiants incorrects", content = @Content)
    })
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

}