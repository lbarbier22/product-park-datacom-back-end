package com.productpark.datacom.dto.response;

import com.productpark.datacom.model.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Version allégée utilisée pour l'écran "Liste des produits" (specs 3.2).
 * Ne contient volontairement pas tous les champs métier, pour éviter
 * de transférer des données inutiles à cet écran.
 */
@Data
@AllArgsConstructor
public class ProductListResponse {

    private Long id;
    private String name;
    private ProductStatus status;
    private Integer currentStep;
    private String createdByLogin;
    private LocalDateTime createdAt;

}
