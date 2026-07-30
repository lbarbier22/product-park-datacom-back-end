package com.productpark.datacom.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO utilisé par PUT /api/products/{id}/step/{stepNumber}.
 *
 * Tous les champs sont optionnels au niveau du DTO : la validation
 * "obligatoire selon le step" (ex: name/reference obligatoires au step 1)
 * est faite dans ProductWorkflowService, car elle dépend du numéro de step
 * et ne peut pas être exprimée simplement avec des annotations Bean Validation.
 *
 * NOTE : l'architecture prévoyait un DTO distinct par step (ProductStep1Request,
 * ProductStep2Request, ...). On est parti ici sur un DTO unique plus simple à
 * maintenir à ce stade du projet - à revoir si la logique par step devient plus complexe.
 */
@Data
public class ProductStepRequest {

    // Step 1
    @Size(min = 3, max = 100)
    private String name;

    private String reference;

    @Size(max = 1000)
    private String description;

    // Step 2
    private String category;
    private String subcategory;

    @Size(max = 150)
    private String manufacturer;

    private String country;

    // Step 3
    private String lot;
    private String certification;

}
