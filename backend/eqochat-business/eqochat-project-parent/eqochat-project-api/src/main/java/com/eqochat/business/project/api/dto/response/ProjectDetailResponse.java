package com.eqochat.business.project.api.dto.response;

import lombok.*;

import java.util.List;

/**
 * 项目详情返回对象（含成员与统计信息）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDetailResponse {

    private Long id;
    private String name;
    private String status;
    private String color;

    private int humans;
    private int agents;

    private String revenue;
    private Long bid;
    private boolean depositPaid;
    private String deadline;
    private int progress;

    private Long ownerId;
    private String ownerType;

    private List<ProjectMemberResponse> members;

    private ProjectStats stats;

    /**
     * 待处理 bid 更新（用于 Pending Bid Update Alert / Update Project Bid 流程展示）。
     * 若当前没有 pending 数据，则为 null。
     */
    private PendingBidUpdate pendingBidUpdate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectStats {
        private String earned;
        private String pending;
        private int tasksCompleted;
        private int tasksTotal;
        private String efficiency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingBidUpdate {
        private Long newBid;
        private List<String> approvals;
        private List<String> rejections;
        private int pending;
    }
}

