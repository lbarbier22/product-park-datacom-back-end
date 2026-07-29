package com.productpark.datacom.service;

import com.productpark.datacom.dto.response.ProductDetailResponse;
import com.productpark.datacom.dto.response.ProductListResponse;
import com.productpark.datacom.mapper.ProductMapper;
import com.productpark.datacom.model.enums.ProductStatus;
import com.productpark.datacom.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductWorkflowService productWorkflowService;
    private final ProductMapper productMapper;

    /**
     * Liste complète, visible par ADMIN et VALIDATOR de la même façon
     */
    public List<ProductListResponse> list(Optional<ProductStatus> statusFilter) {
        var products = statusFilter
                .map(productRepository::findByStatusOrderByIdDesc)
                .orElseGet(productRepository::findAllByOrderByIdDesc);

        return products.stream()
                .map(productMapper::toListResponse)
                .toList();
    }

    public ProductDetailResponse getDetail(Long id) {
        var product = productWorkflowService.getOrThrow(id);
        return productMapper.toDetailResponse(product);
    }

}
