package vn.edu.hcmuaf.fit.coffee_shop.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateUserRequest {
    
    @Size(min = 2, message = "Họ tên phải có ít nhất 2 ký tự")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    private String email;

    private String phoneNumber;

    private String address;

    private String role;

    private Boolean enabled;

    private Boolean locked;
}