package com.productpark.datacom.exception;

/**
 * Levée par exemple quand le VALIDATOR tente de valider/refuser un produit
 * qui n'est pas au statut PENDING, ou quand un ADMIN tente d'éditer un
 * produit PENDING ou VALIDATED (cf. specs fonctionnelles, section 5).
 */
public class ForbiddenTransitionException extends RuntimeException {

    public ForbiddenTransitionException(String message) {
        super(message);
    }

}
