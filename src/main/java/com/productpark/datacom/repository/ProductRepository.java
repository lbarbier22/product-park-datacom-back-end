package com.productpark.datacom.repository;

import com.productpark.datacom.model.entity.Product;
import com.productpark.datacom.model.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Utilisé pour le filtre par statut sur la liste des produits (specs 3.2)
    List<Product> findByStatusOrderByIdDesc(ProductStatus status);

    List<Product> findAllByOrderByIdDesc();

}
