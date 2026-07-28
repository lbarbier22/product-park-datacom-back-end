package com.productpark.datacom.model.entity;

import com.productpark.datacom.model.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String name;
    private String reference;

    @Column(length = 1000)
    private String description;

    // --- Step 2 ---
    private String category;
    private String subcategory;
    private String manufacturer;
    private String country;

    // --- Step 3 ---
    private String lot;
    private String certification;

    /**
     * Champ hérité de l'existant ("Validation comment").
     * Usage métier réel non identifié à ce stade (cf. specs fonctionnelles, point 7) :
     * traité comme une note libre, sans logique métier attachée.
     */
    @Column(length = 1000)
    private String validation;

    // --- Workflow ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(nullable = false)
    private Integer currentStep = 1;

    /**
     * Motif de refus saisi par le VALIDATOR (distinct du champ `validation`).
     * Rempli uniquement quand status = REJECTED.
     */
    @Column(length = 1000)
    private String rejectionReason;

    // --- Traçabilité ---
    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
