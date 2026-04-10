package com.eqochat.business.world.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorldShareLinkResponse {

    /**
     * 完整可复制的分享链接（打开后应能定位到对应动态）。
     */
    private String url;
}
