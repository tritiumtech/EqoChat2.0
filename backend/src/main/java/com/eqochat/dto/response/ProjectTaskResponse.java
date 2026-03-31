package com.eqochat.dto.response;

import lombok.*;

/**
 * 项目侧栏任务返回对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTaskResponse {
    private Long id;
    private String title;
    private String assignee;
    private boolean isAgent;
    private String deadline;
    private String status;
    private String priority;
}

