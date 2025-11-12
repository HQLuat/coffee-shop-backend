package vn.edu.hcmuaf.fit.coffee_shop.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String message;
    private String token;
    private String refreshToken;
}
