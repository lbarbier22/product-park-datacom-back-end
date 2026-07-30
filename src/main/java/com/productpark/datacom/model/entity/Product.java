package com.productpark.datacom.model.entity;

import com.productpark.datacom.model.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Step 1 ---
    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String reference;

    @Column(columnDefinition = "text")
    private String description;

    // --- Step 2 ---
    @Column(length = 100)
    private String category;

    @Column(length = 100)
    private String subcategory;

    @Column(length = 150)
    private String manufacturer;

    @Column(columnDefinition = "char(2)")
    private String country;

    // --- Step 3 ---
    @Column(length = 50)
    private String lot;

    @Column(length = 100)
    private String certification;

    /**
     * Champ hérité de l'existant ("Validation comment").
     * Usage métier réel non identifié à ce stade (cf. specs fonctionnelles, point 7) :
     * traité comme une note libre, sans logique métier attachée.
     */
    @Column(columnDefinition = "text")
    private String validation;

    // --- Workflow ---
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "product_status")
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(name = "current_step", nullable = false)
    private Integer currentStep = 1;

    /**
     * Motif de refus saisi par le VALIDATOR (distinct du champ `validation`).
     * Rempli uniquement quand status = REJECTED.
     */
    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    // --- Traçabilité ---
    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "validated_by")
    private User validatedBy;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime updatedAt;

}