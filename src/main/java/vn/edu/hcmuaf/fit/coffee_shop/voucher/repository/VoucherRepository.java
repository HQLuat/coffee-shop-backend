package vn.edu.hcmuaf.fit.coffee_shop.voucher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.coffee_shop.voucher.entity.Voucher;
import vn.edu.hcmuaf.fit.coffee_shop.voucher.entity.VoucherStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    
    Optional<Voucher> findByCode(String code);
    
    boolean existsByCode(String code);
    
    List<Voucher> findByStatus(VoucherStatus status);
    
    @Query("SELECT v FROM Voucher v WHERE v.status = 'ACTIVE' " +
           "AND v.startDate <= :now AND v.endDate >= :now " +
           "AND v.usedCount < v.totalUsageLimit")
    List<Voucher> findAllActiveVouchers(@Param("now") LocalDateTime now);
}