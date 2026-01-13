package vn.edu.hcmuaf.fit.coffee_shop.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.hcmuaf.fit.coffee_shop.product.entity.Product;
import java.util.List;
// Thêm query trong ProductRepository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByName(String name);  // Tìm theo tên để lấy tất cả size
}