package vn.edu.hcmuaf.fit.coffee_shop.user.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.edu.hcmuaf.fit.coffee_shop.user.entity.Role;
import vn.edu.hcmuaf.fit.coffee_shop.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    // ===== METHODS FOR ADMIN USER MANAGEMENT =====
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchByKeyword(@Param("keyword") String keyword);
    
    List<User> findByRole(Role role);
    
    Long countByRole(Role role);
    
    List<User> findByLocked(Boolean locked);
    
    Long countByLocked(Boolean locked);
    
    List<User> findByEnabled(Boolean enabled);
    
    Long countByEnabled(Boolean enabled);
    
    List<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT u FROM User u WHERE u.lastLoginAt >= :since ORDER BY u.lastLoginAt DESC")
    List<User> findRecentlyLoggedInUsers(@Param("since") LocalDateTime since);
    
    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :since OR u.lastLoginAt IS NULL")
    List<User> findInactiveUsers(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(u) FROM User u WHERE DATE(u.createdAt) = CURRENT_DATE")
    Long countUsersRegisteredToday();
    
    @Query("SELECT COUNT(u) FROM User u WHERE YEARWEEK(u.createdAt, 1) = YEARWEEK(CURRENT_DATE, 1)")
    Long countUsersRegisteredThisWeek();
    
    @Query("SELECT COUNT(u) FROM User u WHERE MONTH(u.createdAt) = MONTH(CURRENT_DATE) " +
           "AND YEAR(u.createdAt) = YEAR(CURRENT_DATE)")
    Long countUsersRegisteredThisMonth();
}