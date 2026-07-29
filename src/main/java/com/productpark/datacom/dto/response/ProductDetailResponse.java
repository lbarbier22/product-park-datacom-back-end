package com.productpark.datacom.dto.response;

import com.productpark.datacom.model.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class ProductDetailResponse {

    private Long id;

    private String name;
    private String reference;
    private String description;

    private String category;
    private String subcategory;
    private String manufacturer;
    private String country;

    private String lot;
    private String certification;
    private String validation;

    private ProductStatus status;
    private Integer currentStep;

    // Rempli uniquement si status = REJECTED (cf. specs, bandeau persistant côté ADMIN)
    private String rejectionReason;

    private String createdByLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
