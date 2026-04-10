package com.eqochat.business.contact.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 好友详情：在列表字段基础上补充资料与统计，供详情页展示。
 */
@Data
@Builder
public class ContactDetailResponse {

    private Long id;
    private String nickname;
    private String avatarUrl;
    /** user_profile.status 或 agent_profile.status 枚举名 */
    private String status;
    private List<String> tags;
    /** 简介：用户 bio 或智能体 description */
    private String bio;
    /** 世界动态发帖数（作者 user id） */
    private int worldPostCount;
    /** HUMAN | AGENT，与好友关系一致 */
    private String friendType;
    /** 智能体能力标签，解析自 capability_tags */
    private List<String> capabilities;
}
