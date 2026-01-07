package vn.edu.hcmuaf.fit.coffee_shop.voucher.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.coffee_shop.voucher.dto.*;
import vn.edu.hcmuaf.fit.coffee_shop.voucher.service.VoucherService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @PostMapping
    public ResponseEntity<VoucherResponse> createVoucher(
            @Valid @RequestBody CreateVoucherRequest request,
            Authentication authentication) {
        try {
            VoucherResponse response = voucherService.createVoucher(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/active")
    public ResponseEntity<List<VoucherResponse>> getActiveVouchers(
            Authentication authentication) {
        String email = authentication != null 
            ? (String) authentication.getPrincipal() 
            : null;
        
        List<VoucherResponse> vouchers = voucherService.getActiveVouchers(email);
        return ResponseEntity.ok(vouchers);
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> getVoucherByCode(
            @PathVariable String code,
            Authentication authentication) {
        try {
            String email = (String) authentication.getPrincipal();
            VoucherResponse response = voucherService.getVoucherByCode(code, email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/apply")
    public ResponseEntity<?> applyVoucher(
            @RequestBody ApplyVoucherRequest request,
            Authentication authentication) {
        try {
            String email = (String) authentication.getPrincipal();
            ApplyVoucherResponse response = voucherService
                    .applyVoucherToCart(email, request.getVoucherCode());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApplyVoucherResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVoucher(
            @PathVariable Long id,
            @Valid @RequestBody CreateVoucherRequest request,
            Authentication authentication) {
        try {
            VoucherResponse response = voucherService.updateVoucher(id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivateVoucher(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            voucherService.deactivateVoucher(id);
            return ResponseEntity.ok(Map.of("message", "Đã vô hiệu hóa voucher"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
}