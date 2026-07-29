package com.productpark.datacom.controller;

import com.productpark.datacom.dto.request.ProductRejectRequest;
import com.productpark.datacom.dto.request.ProductStepRequest;
import com.productpark.datacom.dto.response.ProductDetailResponse;
import com.productpark.datacom.exception.ResourceNotFoundException;
import com.productpark.datacom.mapper.ProductMapper;
import com.productpark.datacom.model.entity.User;
import com.productpark.datacom.repository.UserRepository;
import com.productpark.datacom.service.ProductWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductWorkflowService productWorkflowService;
    private final ProductMapper productMapper;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDetailResponse create(Authentication authentication) {
        User admin = currentUser(authentication);
        var product = productWorkflowService.createDraft(admin);
        return productMapper.toDetailResponse(product);
    }

    @PutMapping("/{id}/step/{stepNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDetailResponse saveStep(@PathVariable("id") Long id,
                                          @PathVariable("stepNumber") int stepNumber,
                                          @RequestParam(value = "next", defaultValue = "false") boolean next,
                                          @Valid @RequestBody ProductStepRequest request) {
        var product = productWorkflowService.saveStep(id, stepNumber, request, next);
        return productMapper.toDetailResponse(product);
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByLogin(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasRole('VALIDATOR')")
    public ProductDetailResponse validate(@PathVariable("id") Long id) {
        var product = productWorkflowService.validate(id);
        return productMapper.toDetailResponse(product);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('VALIDATOR')")
    public ProductDetailResponse reject(@PathVariable("id") Long id,
                                        @Valid @RequestBody ProductRejectRequest request) {
        var product = productWorkflowService.reject(id, request);
        return productMapper.toDetailResponse(product);
    }

}