package vn.edu.hcmuaf.fit.coffee_shop.product.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmuaf.fit.coffee_shop.product.dto.ProductRequest;
import vn.edu.hcmuaf.fit.coffee_shop.product.dto.ProductResponse;
import vn.edu.hcmuaf.fit.coffee_shop.product.service.ProductService;
import vn.edu.hcmuaf.fit.coffee_shop.user.service.CloudinaryService;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin 
public class ProductController {

    private final ProductService service;
    private final CloudinaryService cloudinaryService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/{id}/variants")
    public ResponseEntity<List<ProductResponse>> getVariants(@PathVariable Long id) {
        return ResponseEntity.ok(service.getProductVariants(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> create(
            @RequestPart("product") ProductRequest request,
            @RequestPart("file") MultipartFile file) throws IOException {
        
        String imageUrl = cloudinaryService.uploadProductImage(file);
        request.setImageUrl(imageUrl);
        
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @RequestPart("product") ProductRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        
        ProductResponse oldProduct = service.getById(id);

        if (file != null && !file.isEmpty()) {
            if (oldProduct.getImageUrl() != null) {
                cloudinaryService.deleteProductImage(oldProduct.getImageUrl());
            }
            String newImageUrl = cloudinaryService.uploadProductImage(file);
            request.setImageUrl(newImageUrl);
        } else {
            request.setImageUrl(oldProduct.getImageUrl());
        }
        
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        ProductResponse product = service.getById(id);
        if (product != null && product.getImageUrl() != null) {
            cloudinaryService.deleteProductImage(product.getImageUrl());
        }
        
        service.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}