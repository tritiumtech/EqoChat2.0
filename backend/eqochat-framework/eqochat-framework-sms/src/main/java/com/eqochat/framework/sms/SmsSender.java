package com.eqochat.framework.sms;

/**
 * 短信发送网关接口。
 * <p>
 * 通过抽象接口的方式定义短信发送能力，便于后续替换为不同服务商实现，
 * 同时在开发与测试环境中可以使用轻量的模拟实现。
 */
public interface SmsSender {

    /**
     * 发送短信验证码。
     *
     * @param phone 接收验证码的手机号
     * @param code  待发送的验证码内容
     */
    void sendVerificationCode(String phone, String code);
}

