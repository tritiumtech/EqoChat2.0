package com.eqochat.framework.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 默认短信发送实现，仅在日志中输出验证码。
 * <p>
 * 适用于开发与测试环境；在生产环境中可以通过自定义 {@link SmsSender}
 * Bean 覆盖本实现以接入真实短信服务商。
 */
@Service
@Slf4j
public class LoggingSmsSender implements SmsSender {

    @Override
    public void sendVerificationCode(String phone, String code) {
        log.info("模拟发送短信验证码: phone={}, code={}", phone, code);
    }
}

