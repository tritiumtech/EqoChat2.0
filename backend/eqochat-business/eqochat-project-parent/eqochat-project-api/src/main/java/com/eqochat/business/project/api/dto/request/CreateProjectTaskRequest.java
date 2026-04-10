package com.eqochat.business.project.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * 创建项目任务请求体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectTaskRequest {

    @NotBlank(message = "任务标题不能为空")
    private String title;

    @NotNull(message = "任务价值不能为空")
    @Min(value = 1, message = "任务价值必须大于0")
    private Long value;

    @NotBlank(message = "截止日期不能为空")
    private String deadline;

    @NotBlank(message = "优先级不能为空")
    private String priority;
}