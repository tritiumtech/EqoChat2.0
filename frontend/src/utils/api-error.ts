/**
 * 与后端 {@code com.eqochat.common.ApiErrorCodes} 及 {@code ApiResponse} 约定对齐。
 * 业务失败时 body 含 {@code code}、{@code message}，消息键形态时另有 {@code errorCode}。
 * 仅 {@link API_ERROR_AUTH_TOKEN_INVALID} 表示应整站登出并跳转登录（与 BizException 抛出点一致）。
 */
export const API_CODE_UNAUTHORIZED = 401

/** 与后端 ApiErrorCodes.AUTH_TOKEN_INVALID 一致 */
export const API_ERROR_AUTH_TOKEN_INVALID = 'auth.token.invalid'

/**
 * 是否应根据 JSON 业务体强制重新登录。
 * 不以 body.code===401 单独判断，避免与未来其它 401 业务码混淆；令牌问题统一看 errorCode。
 */
export function shouldForceReloginPayload(payload: { code?: number; errorCode?: string }): boolean {
  return payload.errorCode === API_ERROR_AUTH_TOKEN_INVALID
}
