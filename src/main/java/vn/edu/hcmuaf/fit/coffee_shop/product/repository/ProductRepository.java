package vn.edu.hcmuaf.fit.coffee_shop.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.hcmuaf.fit.coffee_shop.product.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
