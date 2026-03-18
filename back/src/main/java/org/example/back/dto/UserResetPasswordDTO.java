package org.example.back.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserResetPasswordDTO {

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
