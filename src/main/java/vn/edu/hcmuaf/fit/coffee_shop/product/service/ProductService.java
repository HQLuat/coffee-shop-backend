package vn.edu.hcmuaf.fit.coffee_shop.product.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmuaf.fit.coffee_shop.product.dto.ProductRequest;
import vn.edu.hcmuaf.fit.coffee_shop.product.dto.ProductResponse;
import vn.edu.hcmuaf.fit.coffee_shop.product.entity.Category;
import vn.edu.hcmuaf.fit.coffee_shop.product.entity.Product;
import vn.edu.hcmuaf.fit.coffee_shop.product.entity.Size;
import vn.edu.hcmuaf.fit.coffee_shop.product.repository.ProductRepository;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;

    // them product
    public ProductResponse create(ProductRequest r) {
        Product p = mapToEntity(r);
        repository.save(p);
        return mapToResponse(p);
    }

    // sua product
    public ProductResponse update(Long id, ProductRequest r) {
        Product p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        p.setName(r.getName());
        p.setPrice(r.getPrice());
        p.setDescription(r.getDescription());
        p.setImageUrl(r.getImageUrl());
        p.setCategory(Category.valueOf(r.getCategory()));
        p.setSize(Size.valueOf(r.getSize()));
        repository.save(p);
        return mapToResponse(p);
    }

    // xoa
    public void delete(Long id) {
        repository.deleteById(id);
    }

    // get all
    public List<ProductResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // get by id
    public ProductResponse getById(Long id) {
        Product p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return mapToResponse(p);
    }

    private Product mapToEntity(ProductRequest r) {
        return Product.builder()
                .name(r.getName())
                .price(r.getPrice())
                .description(r.getDescription())
                .imageUrl(r.getImageUrl())
                .category(Category.valueOf(r.getCategory()))
                .size(Size.valueOf(r.getSize()))
                .build();
    }

    private ProductResponse mapToResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .price(p.getPrice())
                .imageUrl(p.getImageUrl())
                .category(p.getCategory().name())
                .size(p.getSize().name())
                 .description(p.getDescription())  // THÊM DÒNG NÀY
                .build();
    }
}
