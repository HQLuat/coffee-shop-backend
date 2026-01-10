package vn.edu.hcmuaf.fit.coffee_shop.user.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatisticsResponse {
    private Long totalUsers;
    private Long activeUsers;
    private Long lockedUsers;
    private Long unverifiedUsers;
    private Long adminUsers;
    private Long regularUsers;
    private Long newUsersThisMonth;
    private Long newUsersToday;
    private Double activeUserPercentage;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}