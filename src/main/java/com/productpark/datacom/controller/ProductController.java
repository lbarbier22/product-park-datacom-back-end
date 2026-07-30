package com.productpark.datacom.controller;

import com.productpark.datacom.dto.request.ProductRejectRequest;
import com.productpark.datacom.dto.request.ProductStepRequest;
import com.productpark.datacom.dto.response.ProductDetailResponse;
import com.productpark.datacom.dto.response.ProductListResponse;
import com.productpark.datacom.exception.ResourceNotFoundException;
import com.productpark.datacom.mapper.ProductMapper;
import com.productpark.datacom.model.entity.User;
import com.productpark.datacom.model.enums.ProductStatus;
import com.productpark.datacom.repository.UserRepository;
import com.productpark.datacom.service.ProductService;
import com.productpark.datacom.service.ProductWorkflowService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Produits", description = "Gestion des produits et de leur workflow de validation")
public class ProductController {

    private final ProductService productService;
    private final ProductWorkflowService productWorkflowService;
    private final ProductMapper productMapper;
    private final UserRepository userRepository;

    @Operation(
            summary = "Créer un brouillon de produit",
            description = "Initialise la création d'un nouveau produit sous forme de brouillon (Accès réservé aux ADMINS)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Brouillon créé avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé (Rôle ADMIN requis)"),
            @ApiResponse(responseCode = "404", description = "Utilisateur administrateur introuvable")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDetailResponse create(Authentication authentication) {
        User admin = currentUser(authentication);
        var product = productWorkflowService.createDraft(admin);
        return productMapper.toDetailResponse(product);
    }

    @Operation(
            summary = "Sauvegarder une étape de création/édition",
            description = "Enregistre les données d'une étape spécifique du workflow pour un produit donné (Accès réservé aux ADMINS)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Étape enregistrée avec succès"),
            @ApiResponse(responseCode = "400", description = "Données d'étape invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé (Rôle ADMIN requis)"),
            @ApiResponse(responseCode = "404", description = "Produit non trouvé")
    })
    @PutMapping("/{id}/step/{stepNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDetailResponse saveStep(
            @Parameter(description = "ID du produit", required = true) @PathVariable("id") Long id,
            @Parameter(description = "Numéro de l'étape", required = true) @PathVariable("stepNumber") int stepNumber,
            @Parameter(description = "Indique s'il faut passer directement à l'étape suivante") @RequestParam(value = "next", defaultValue = "false") boolean next,
            @Valid @RequestBody ProductStepRequest request) {
        var product = productWorkflowService.saveStep(id, stepNumber, request, next);
        return productMapper.toDetailResponse(product);
    }

    @Operation(
            summary = "Valider un produit",
            description = "Passe le statut d'un produit à 'Validé' (Accès réservé aux VALIDATEURS)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produit validé avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé (Rôle VALIDATOR requis)"),
            @ApiResponse(responseCode = "404", description = "Produit non trouvé")
    })
    @PostMapping("/{id}/validate")
    @PreAuthorize("hasRole('VALIDATOR')")
    public ProductDetailResponse validate(
            @Parameter(description = "ID du produit à valider", required = true) @PathVariable("id") Long id) {
        var product = productWorkflowService.validate(id);
        return productMapper.toDetailResponse(product);
    }

    @Operation(
            summary = "Rejeter un produit",
            description = "Rejette un produit en fournissant un motif dans la requête (Accès réservé aux VALIDATEURS)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produit rejeté avec succès"),
            @ApiResponse(responseCode = "400", description = "Motif de rejet manquant ou invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé (Rôle VALIDATOR requis)"),
            @ApiResponse(responseCode = "404", description = "Produit non trouvé")
    })
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('VALIDATOR')")
    public ProductDetailResponse reject(
            @Parameter(description = "ID du produit à rejeter", required = true) @PathVariable("id") Long id,
            @Valid @RequestBody ProductRejectRequest request) {
        var product = productWorkflowService.reject(id, request);
        return productMapper.toDetailResponse(product);
    }

    @Operation(
            summary = "Lister les produits",
            description = "Récupère la liste de tous les produits, optionnellement filtrée par statut."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des produits récupérée avec succès")
    })
    @GetMapping
    public List<ProductListResponse> list(
            @Parameter(description = "Filtrer optionnellement par statut du produit")
            @RequestParam(value = "status", required = false) ProductStatus status) {
        return productService.list(Optional.ofNullable(status));
    }

    @Operation(
            summary = "Obtenir les détails d'un produit",
            description = "Retourne toutes les informations détaillées d'un produit spécifique."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Détails du produit récupérés avec succès"),
            @ApiResponse(responseCode = "404", description = "Produit non trouvé")
    })
    @GetMapping("/{id}")
    public ProductDetailResponse getDetail(
            @Parameter(description = "ID du produit", required = true) @PathVariable("id") Long id) {
        return productService.getDetail(id);
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByLogin(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

}