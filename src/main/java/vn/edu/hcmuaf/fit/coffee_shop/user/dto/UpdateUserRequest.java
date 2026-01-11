package vn.edu.hcmuaf.fit.coffee_shop.user.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    
    @Size(min = 2, message = "Họ tên phải có ít nhất 2 ký tự")
    private String fullName;

    private String phoneNumber;

    private String address;

    private String avatarUrl;

    private String currentPassword;

    private String newPassword;

    private String confirmNewPassword;
}
