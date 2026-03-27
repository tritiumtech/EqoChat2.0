package com.eqochat.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 消息附件返回（FILE/IMAGE/CARD 等）
 */
@Data
@Builder
public class MessageAttachmentResponse {

    private String fileName;
    private String fileSize;
    private String fileType;

    /**
     * 真实下载能力在 Phase 2 文件服务后启用
     */
    private String downloadUrl;
}

