package com.productpark.datacom.service;

import com.productpark.datacom.dto.request.ProductRejectRequest;
import com.productpark.datacom.dto.request.ProductStepRequest;
import com.productpark.datacom.exception.ForbiddenTransitionException;
import com.productpark.datacom.exception.ResourceNotFoundException;
import com.productpark.datacom.model.entity.Product;
import com.productpark.datacom.model.entity.User;
import com.productpark.datacom.model.enums.ProductStatus;
import com.productpark.datacom.model.enums.Role;
import com.productpark.datacom.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductWorkflowServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductWorkflowService productWorkflowService;

    private User adminA;
    private User adminB;

    @BeforeEach
    void setUp() {
        adminA = new User(1L, "adminA", "hash", "Alice", "A", Role.ADMIN);
        adminB = new User(2L, "adminB", "hash", "Bob", "B", Role.ADMIN);

        // save() renvoie l'entité telle quelle (comportement par défaut simulé)
        lenient().when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Product draftProduct(User creator) {
        Product product = new Product();
        product.setId(10L);
        product.setCreatedBy(creator);
        product.setStatus(ProductStatus.DRAFT);
        product.setCurrentStep(1);
        return product;
    }

    // --- Transitions valides ---

    @Test
    void step4_withNext_movesStatusToPending() {
        Product product = draftProduct(adminA);
        product.setCurrentStep(4);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        Product result = productWorkflowService.saveStep(10L, 4, new ProductStepRequest(), true);

        assertThat(result.getStatus()).isEqualTo(ProductStatus.PENDING);
        assertThat(result.getRejectionReason()).isNull();
    }

    @Test
    void validate_onPendingProduct_movesToValidated() {
        Product product = draftProduct(adminA);
        product.setStatus(ProductStatus.PENDING);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        Product result = productWorkflowService.validate(10L);

        assertThat(result.getStatus()).isEqualTo(ProductStatus.VALIDATED);
    }

    @Test
    void reject_onPendingProduct_movesToRejectedWithReason() {
        Product product = draftProduct(adminA);
        product.setStatus(ProductStatus.PENDING);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        ProductRejectRequest request = new ProductRejectRequest();
        request.setRejectionReason("Référence produit incorrecte");

        Product result = productWorkflowService.reject(10L, request);

        assertThat(result.getStatus()).isEqualTo(ProductStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Référence produit incorrecte");
    }

    // --- Transitions invalides ---

    @Test
    void editingPendingProduct_throwsForbiddenTransition() {
        Product product = draftProduct(adminA);
        product.setStatus(ProductStatus.PENDING);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productWorkflowService.saveStep(10L, 1, new ProductStepRequest(), false))
                .isInstanceOf(ForbiddenTransitionException.class);
    }

    @Test
    void editingValidatedProduct_throwsForbiddenTransition() {
        Product product = draftProduct(adminA);
        product.setStatus(ProductStatus.VALIDATED);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productWorkflowService.saveStep(10L, 1, new ProductStepRequest(), false))
                .isInstanceOf(ForbiddenTransitionException.class);
    }

    @Test
    void validate_onDraftProduct_throwsForbiddenTransition() {
        Product product = draftProduct(adminA); // reste en DRAFT
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productWorkflowService.validate(10L))
                .isInstanceOf(ForbiddenTransitionException.class);
    }

    @Test
    void reject_onDraftProduct_throwsForbiddenTransition() {
        Product product = draftProduct(adminA); // reste en DRAFT
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        ProductRejectRequest request = new ProductRejectRequest();
        request.setRejectionReason("Motif quelconque");

        assertThatThrownBy(() -> productWorkflowService.reject(10L, request))
                .isInstanceOf(ForbiddenTransitionException.class);
    }

    @Test
    void unknownProduct_throwsResourceNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productWorkflowService.getOrThrow(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- Reset après refus ---

    @Test
    void editingRejectedProduct_resetsToDraftStep1() {
        Product product = draftProduct(adminA);
        product.setStatus(ProductStatus.REJECTED);
        product.setCurrentStep(4); // était au step 4 au moment du refus
        product.setRejectionReason("À corriger");
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        ProductStepRequest request = new ProductStepRequest();
        request.setName("Nom corrigé");

        Product result = productWorkflowService.saveStep(10L, 1, request, false);

        assertThat(result.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(result.getCurrentStep()).isEqualTo(1);
        // le motif reste visible tant que le step 4 n'est pas re-soumis
        assertThat(result.getRejectionReason()).isEqualTo("À corriger");
    }

    @Test
    void resubmittingStep4AfterRejection_purgesRejectionReason() {
        Product product = draftProduct(adminA);
        product.setStatus(ProductStatus.REJECTED);
        product.setCurrentStep(1);
        product.setRejectionReason("À corriger");
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        Product result = productWorkflowService.saveStep(10L, 4, new ProductStepRequest(), true);

        assertThat(result.getStatus()).isEqualTo(ProductStatus.PENDING);
        assertThat(result.getRejectionReason()).isNull();
    }

    // --- Édition croisée entre ADMIN ---

    @Test
    void adminB_canEditProductCreatedByAdminA() {
        Product product = draftProduct(adminA); // créé par adminA
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        ProductStepRequest request = new ProductStepRequest();
        request.setName("Édité par un autre admin");

        // Aucune vérification de créateur dans getEditableOrThrow/saveStep :
        // n'importe quel ADMIN authentifié peut éditer (décision actée dans les specs).
        Product result = productWorkflowService.saveStep(10L, 1, request, false);

        assertThat(result.getName()).isEqualTo("Édité par un autre admin");
        assertThat(result.getCreatedBy()).isEqualTo(adminA); // le créateur d'origine ne change pas
    }

}
