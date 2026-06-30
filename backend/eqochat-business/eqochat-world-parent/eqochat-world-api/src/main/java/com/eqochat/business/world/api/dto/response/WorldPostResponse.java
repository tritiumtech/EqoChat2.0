package com.eqochat.business.world.api.dto.response;

import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorldPostResponse {

    private String id;
    private Author author;
    private String content;
    private String mediaType;
    private String imageUrl;
    private String videoUrl;
    private SharedProject sharedProject;
    private String timestamp;
    /**
     * 完整的时间戳（ISO-8601 格式），用于前端计算年/月/日分组
     */
    private String createdAt;
    private int upvotes;
    private int replies;
    private List<String> topics;
    private boolean upvoted;
    private boolean friend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Author {
        private Long id;
        private String name;
        /**
         * Author subject type. Sprint 1A stores World authors as human users,
         * but exposing this keeps the API ready for agent authors without
         * making the frontend infer from the display name.
         */
        private String type;
        /**
         * 用于前端渐变/头像色（hex），与 Figma Make 示例保持一致。
         */
        private String avatar;
        private boolean ai;
        private Long associatedHumanId;
        private String associatedHumanName;
        private String walletRouting;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SharedProject {
        private String id;
        private String name;
        private String ownerName;
        private boolean ownerAi;
        private String associatedHumanName;
        private String budget;
        private String teamMix;
        private String deadline;
        private String status;
    }

    public static String formatTime(LocalDateTime time) {
        if (time == null) return "";
        Duration d = Duration.between(time, LocalDateTime.now());
        long mins = Math.max(0, d.toMinutes());
        if (mins < 1) return "now";
        if (mins < 60) return mins + "m";
        long hours = d.toHours();
        if (hours < 24) return hours + "h";
        long days = d.toDays();
        if (days < 7) return days + "d";
        return time.toLocalDate().toString();
    }
}
