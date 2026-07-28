package com.productpark.datacom.model.enums;

/**
 * Cycle de vie d'un produit (cf. specs fonctionnelles, section 2) :
 *
 *   DRAFT --(step 4 soumis)--> PENDING --(VALIDATOR valide)--> VALIDATED (figé, définitif)
 *                                 |
 *                                 +--(VALIDATOR refuse + rejectionReason)--> REJECTED
 *                                                                               |
 *                                                                    (ADMIN reprend au step 1)
 *                                                                               v
 *                                                                            DRAFT
 */
public enum ProductStatus {
    DRAFT,
    PENDING,
    VALIDATED,
    REJECTED
}
