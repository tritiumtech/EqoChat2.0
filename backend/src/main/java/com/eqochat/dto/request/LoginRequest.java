package com.eqochat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户登录请求
 */
@Data
public class LoginRequest {
    
    @NotBlank(message = "{auth.phone.required}")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "{auth.phone.invalid}")
    private String phone;
    
    @NotBlank(message = "{auth.password.required}")
    @Size(min = 6, max = 20, message = "{auth.password.length}")
    private String password;
}
