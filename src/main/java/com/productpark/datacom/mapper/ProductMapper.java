package com.productpark.datacom.mapper;

import com.productpark.datacom.dto.response.ProductDetailResponse;
import com.productpark.datacom.dto.response.ProductListResponse;
import com.productpark.datacom.model.entity.Product;
import org.springframework.stereotype.Component;

/**
 * Mapping manuel pour l'instant (pas de MapStruct) - le volume de champs
 * reste raisonnable pour ce projet. À reconsidérer si le modèle grossit.
 */
@Component
public class ProductMapper {

    public ProductListResponse toListResponse(Product product) {
        return new ProductListResponse(
                product.getId(),
                product.getName(),
                product.getStatus(),
                product.getCurrentStep(),
                product.getCreatedBy().getLogin(),
                product.getCreatedAt()
        );
    }

    public ProductDetailResponse toDetailResponse(Product product) {
        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .reference(product.getReference())
                .description(product.getDescription())
                .category(product.getCategory())
                .subcategory(product.getSubcategory())
                .manufacturer(product.getManufacturer())
                .country(product.getCountry())
                .lot(product.getLot())
                .certification(product.getCertification())
                .validation(product.getValidation())
                .status(product.getStatus())
                .currentStep(product.getCurrentStep())
                .rejectionReason(product.getRejectionReason())
                .createdByLogin(product.getCreatedBy().getLogin())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

}
