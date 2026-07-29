package com.productpark.datacom.service;

import com.productpark.datacom.dto.request.ProductRejectRequest;
import com.productpark.datacom.dto.request.ProductStepRequest;
import com.productpark.datacom.exception.ForbiddenTransitionException;
import com.productpark.datacom.exception.ResourceNotFoundException;
import com.productpark.datacom.model.entity.Product;
import com.productpark.datacom.model.entity.User;
import com.productpark.datacom.model.enums.ProductStatus;
import com.productpark.datacom.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Centralise toute la logique de workflow (cf. document d'architecture, 1.1) :
 * plus possible de "sauter" un step ou de forcer une transition de statut
 * invalide depuis le frontend, car tout est revérifié ici, côté serveur.
 */
@Service
@RequiredArgsConstructor
public class ProductWorkflowService {

    private static final int LAST_STEP = 4;

    private final ProductRepository productRepository;

    public Product createDraft(User admin) {
        Product product = new Product();
        product.setCreatedBy(admin);
        product.setStatus(ProductStatus.DRAFT);
        product.setCurrentStep(1);
        return productRepository.save(product);
    }

    public Product getEditableOrThrow(Long id) {
        Product product = getOrThrow(id);

        // N'importe quel ADMIN peut éditer n'importe quel produit (décision actée),
        // tant qu'il est en DRAFT ou REJECTED.
        if (product.getStatus() != ProductStatus.DRAFT && product.getStatus() != ProductStatus.REJECTED) {
            throw new ForbiddenTransitionException("Ce produit ne peut pas être modifié dans son état actuel");
        }

        return product;
    }

    public Product saveStep(Long id, int stepNumber, ProductStepRequest request, boolean moveNext) {

        Product product = getEditableOrThrow(id);

        // Si le produit était REJECTED, la reprise d'édition le repasse en DRAFT
        // et réinitialise le step à 1 (décision actée dans les specs, section 2 et 6).
        if (product.getStatus() == ProductStatus.REJECTED) {
            product.setStatus(ProductStatus.DRAFT);
            product.setCurrentStep(1);
            // rejectionReason volontairement conservé tel quel ici (pas touché)
            // pour rester affiché en bandeau pendant toute la ressaisie (specs, section 3.3 / 5).
        }

        applyStepFields(product, stepNumber, request);

        if (moveNext) {
            int next = Math.min(stepNumber + 1, LAST_STEP);
            product.setCurrentStep(next);

            if (stepNumber == LAST_STEP) {
                product.setStatus(ProductStatus.PENDING);
                product.setRejectionReason(null); // motif de refus purgé une fois re-soumis
            }
        }

        return productRepository.save(product);
    }

    private void applyStepFields(Product product, int stepNumber, ProductStepRequest request) {
        switch (stepNumber) {
            case 1 -> {
                if (request.getName() != null) product.setName(request.getName());
                if (request.getReference() != null) product.setReference(request.getReference());
                if (request.getDescription() != null) product.setDescription(request.getDescription());
            }
            case 2 -> {
                if (request.getCategory() != null) product.setCategory(request.getCategory());
                if (request.getSubcategory() != null) product.setSubcategory(request.getSubcategory());
                if (request.getManufacturer() != null) product.setManufacturer(request.getManufacturer());
                if (request.getCountry() != null) product.setCountry(request.getCountry());
            }
            case 3 -> {
                if (request.getLot() != null) product.setLot(request.getLot());
                if (request.getCertification() != null) product.setCertification(request.getCertification());
                if (request.getValidation() != null) product.setValidation(request.getValidation());
            }
            case 4 -> {
                // Step de récapitulatif uniquement, aucun champ propre à modifier ici.
            }
            default -> throw new IllegalArgumentException("Step invalide : " + stepNumber);
        }
    }

    public Product validate(Long id) {
        Product product = getOrThrow(id);

        if (product.getStatus() != ProductStatus.PENDING) {
            throw new ForbiddenTransitionException("Seul un produit en attente peut être validé");
        }

        product.setStatus(ProductStatus.VALIDATED);
        return productRepository.save(product);
    }

    public Product reject(Long id, ProductRejectRequest request) {
        Product product = getOrThrow(id);

        if (product.getStatus() != ProductStatus.PENDING) {
            throw new ForbiddenTransitionException("Seul un produit en attente peut être refusé");
        }

        product.setStatus(ProductStatus.REJECTED);
        product.setRejectionReason(request.getRejectionReason());
        return productRepository.save(product);
    }

    public Product getOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));
    }

}
