package com.eqochat.common;

/**
 * 与 {@link ApiResponse}、{@link BizException} 配合使用的稳定错误码常量。
 * <p>
 * 业务错误通过 JSON 返回：{@code code} 为 HTTP 语义类业务码（与 BizException.code 一致），
 * {@code errorCode} 为 i18n 消息键（与 BizException.messageKey 一致）。
 * 会话/令牌无效时统一使用 {@link #AUTH_TOKEN_INVALID}，由前端据此决定是否整站登出。
 */
public final class ApiErrorCodes {

    private ApiErrorCodes() {
    }

    /** 与 HTTP 401 对齐的业务码，表示未认证或令牌无效 */
    public static final int CODE_UNAUTHORIZED = 401;

    /**
     * 令牌无效或缺失（刷新失败、UserContext 要求登录等）。
     * 勿用于普通参数校验；前端仅应在此 errorCode 下执行强制重新登录。
     */
    public static final String AUTH_TOKEN_INVALID = "auth.token.invalid";
}
