package com.eqochat.dto.response;

import lombok.*;

/**
 * 项目侧栏支付返回对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectPaymentResponse {
    private Long id;
    private Long amount;
    private String recipient;
    private boolean isAgent;
    private String masterWallet;
    private String status;
    private String date;
}

