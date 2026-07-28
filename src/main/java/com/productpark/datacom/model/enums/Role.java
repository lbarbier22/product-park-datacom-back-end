package com.productpark.datacom.model.enums;

/**
 * Rôles applicatifs.
 * Le rôle USER de l'ancienne version est volontairement supprimé
 * (bug identifié : un USER pouvait créer/valider des produits, ce qui n'était pas voulu).
 */
public enum Role {
    ADMIN,
    VALIDATOR
}
