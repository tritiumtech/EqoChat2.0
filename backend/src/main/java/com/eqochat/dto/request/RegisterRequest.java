package com.eqochat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求
 */
@Data
public class RegisterRequest {
    
    @NotBlank(message = "{auth.phone.required}")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "{auth.phone.invalid}")
    private String phone;
    
    @NotBlank(message = "{auth.verify_code.required}")
    @Size(min = 4, max = 6, message = "{auth.verify_code.length}")
    private String verifyCode;
    
    @NotBlank(message = "{auth.password.required}")
    @Size(min = 6, max = 20, message = "{auth.password.length}")
    private String password;
    
    @NotBlank(message = "{auth.nickname.required}")
    @Size(min = 2, max = 50, message = "{auth.nickname.length}")
    private String nickname;
    
    private String avatarUrl;
}
