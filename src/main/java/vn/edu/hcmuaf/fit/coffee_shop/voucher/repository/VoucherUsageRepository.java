package vn.edu.hcmuaf.fit.coffee_shop.voucher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.coffee_shop.voucher.entity.VoucherUsage;

import java.util.List;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {
    
    List<VoucherUsage> findByUserId(Long userId);
    
    List<VoucherUsage> findByVoucherId(Long voucherId);
    
    int countByVoucherIdAndUserId(Long voucherId, Long userId);
    
    List<VoucherUsage> findByOrderId(Long orderId);
}