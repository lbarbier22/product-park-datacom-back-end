package com.productpark.datacom.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductRejectRequest {

    @NotBlank
    @Size(min = 10, message = "Le motif de refus doit contenir au moins 10 caractères")
    private String rejectionReason;

}
