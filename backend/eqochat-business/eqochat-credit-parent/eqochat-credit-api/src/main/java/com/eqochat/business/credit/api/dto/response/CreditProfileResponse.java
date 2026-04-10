package com.eqochat.business.credit.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditProfileResponse {

    private int creditScore;
    private int projectsCompleted;
    private int successRate;
    private int disputeCount;

    private List<Dispute> disputes;
    private List<Review> reviews;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dispute {
        private String id;
        private String projectName;
        private String filedBy;
        private String reason;
        /**
         * verified / unverified / pending
         */
        private String verdict;
        private String date;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Review {
        private String id;
        private String projectName;
        private int rating;
        private String comment;
        private String from;
        private String date;
    }
}

