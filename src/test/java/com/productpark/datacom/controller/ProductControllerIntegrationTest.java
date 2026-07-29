package com.productpark.datacom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.productpark.datacom.model.entity.User;
import com.productpark.datacom.model.enums.Role;
import com.productpark.datacom.repository.ProductRepository;
import com.productpark.datacom.repository.UserRepository;
import com.productpark.datacom.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de bout en bout via MockMvc : couvre les codes HTTP réels
 * (401/403/409/400/404) et un scénario nominal complet ADMIN -> VALIDATOR.
 * Base H2 en mémoire (profil "test"), voir application-test.yml.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String adminBToken;
    private String validatorToken;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(new User(null, "admin.test", passwordEncoder.encode("admin123"),
                "Alice", "Admin", Role.ADMIN));
        User adminB = userRepository.save(new User(null, "adminB.test", passwordEncoder.encode("admin123"),
                "Bob", "Admin", Role.ADMIN));
        userRepository.save(new User(null, "validator.test", passwordEncoder.encode("validator123"),
                "Carol", "Validator", Role.VALIDATOR));

        adminToken = jwtTokenProvider.generateToken(admin.getLogin(), "ADMIN");
        adminBToken = jwtTokenProvider.generateToken(adminB.getLogin(), "ADMIN");
        validatorToken = jwtTokenProvider.generateToken("validator.test", "VALIDATOR");
    }

    // --- 401 / 403 ---

    @Test
    void noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/products").header("Authorization", "Bearer invalide"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validatorCreatingProduct_returns403() throws Exception {
        mockMvc.perform(post("/api/products").header("Authorization", "Bearer " + validatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminValidatingProduct_returns403() throws Exception {
        Long id = createDraftProductAsAdmin();
        mockMvc.perform(post("/api/products/" + id + "/validate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    // --- 404 ---

    @Test
    void unknownProduct_returns404WithMessage() throws Exception {
        mockMvc.perform(get("/api/products/99999").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Produit introuvable"));
    }

    // --- Scénario nominal complet ---

    @Test
    void fullNominalScenario_draftToValidated() throws Exception {
        Long id = createDraftProductAsAdmin();

        // Step 1
        mockMvc.perform(put("/api/products/" + id + "/step/1?next=true")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Chaise\",\"reference\":\"REF-001\",\"description\":\"Desc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStep").value(2));

        // Step 2
        mockMvc.perform(put("/api/products/" + id + "/step/2?next=true")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"Mobilier\",\"manufacturer\":\"Acme\",\"country\":\"France\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStep").value(3));

        // Step 3
        mockMvc.perform(put("/api/products/" + id + "/step/3?next=true")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lot\":\"LOT-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStep").value(4));

        // Step 4 -> PENDING
        mockMvc.perform(put("/api/products/" + id + "/step/4?next=true")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Édition croisée : adminB peut aussi éditer avant validation ? Non : status=PENDING -> 409 même pour adminB
        mockMvc.perform(put("/api/products/" + id + "/step/1")
                        .header("Authorization", "Bearer " + adminBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\"}"))
                .andExpect(status().isConflict());

        // Validation par le VALIDATOR
        mockMvc.perform(post("/api/products/" + id + "/validate")
                        .header("Authorization", "Bearer " + validatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"));

        // Impossible d'éditer un produit VALIDATED
        mockMvc.perform(put("/api/products/" + id + "/step/1")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ce produit ne peut pas être modifié dans son état actuel"));
    }

    @Test
    void rejectionScenario_thenCrossAdminResubmission() throws Exception {
        Long id = createDraftProductAsAdmin();
        goToStep4Pending(id, adminToken);

        // Refus avec motif trop court -> 400
        mockMvc.perform(post("/api/products/" + id + "/reject")
                        .header("Authorization", "Bearer " + validatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectionReason\":\"trop crt\"}"))
                .andExpect(status().isBadRequest());

        // Refus avec motif valide
        mockMvc.perform(post("/api/products/" + id + "/reject")
                        .header("Authorization", "Bearer " + validatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectionReason\":\"Référence produit incorrecte, à revoir\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        // Un AUTRE admin (adminB) reprend l'édition -> reset step 1, statut DRAFT, motif conservé
        mockMvc.perform(put("/api/products/" + id + "/step/1")
                        .header("Authorization", "Bearer " + adminBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Corrigé par adminB\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currentStep").value(1))
                .andExpect(jsonPath("$.rejectionReason").value("Référence produit incorrecte, à revoir"));
    }

    // --- Helpers ---

    private Long createDraftProductAsAdmin() throws Exception {
        String response = mockMvc.perform(post("/api/products").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void goToStep4Pending(Long id, String token) throws Exception {
        mockMvc.perform(put("/api/products/" + id + "/step/1?next=true")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Chaise\",\"reference\":\"REF-001\"}"));
        mockMvc.perform(put("/api/products/" + id + "/step/2?next=true")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"Mobilier\"}"));
        mockMvc.perform(put("/api/products/" + id + "/step/3?next=true")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lot\":\"LOT-01\"}"));
        mockMvc.perform(put("/api/products/" + id + "/step/4?next=true")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
    }

}
