package com.eqochat.business.user.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 发送验证码请求
 */
@Data
public class VerifyCodeRequest {
    
    @NotBlank(message = "{auth.phone.required}")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "{auth.phone.invalid}")
    private String phone;
}
