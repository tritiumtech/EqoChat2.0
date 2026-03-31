package com.eqochat.service.impl;

import com.eqochat.domain.entity.AgentProfile;
import com.eqochat.domain.entity.CreditRecord;
import com.eqochat.domain.entity.UserProfile;
import com.eqochat.domain.entity.ViolationRecord;
import com.eqochat.dto.response.CreditProfileResponse;
import com.eqochat.mapper.AgentProfileMapper;
import com.eqochat.mapper.CreditRecordMapper;
import com.eqochat.mapper.UserProfileMapper;
import com.eqochat.mapper.ViolationRecordMapper;
import com.eqochat.service.CreditProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditProfileServiceImpl implements CreditProfileService {

    private final CreditRecordMapper creditRecordMapper;
    private final ViolationRecordMapper violationRecordMapper;
    private final UserProfileMapper userProfileMapper;
    private final AgentProfileMapper agentProfileMapper;

    @Override
    public CreditProfileResponse getSubjectCreditProfile(Long subjectId, String subjectType) {
        String st = subjectType == null ? "" : subjectType.trim().toUpperCase();

        // 1) 拉取记录
        List<CreditRecord> creditRecords = creditRecordMapper.findBySubject(subjectId, st);
        List<ViolationRecord> violationRecords = violationRecordMapper.findBySubject(subjectId, st);

        // 2) 基础分数：优先使用信用记录的 latest current_score
        int creditScore = 0;
        if (!creditRecords.isEmpty() && creditRecords.get(0).getCurrentScore() != null) {
            creditScore = creditRecords.get(0).getCurrentScore();
        } else {
            creditScore = resolveProfileCreditScore(subjectId, st);
        }

        // 3) projectsCompleted：用“与 PROJECT 相关的正向变动”作为近似口径
        int projectsCompleted = 0;
        for (CreditRecord cr : creditRecords) {
            if (cr == null) continue;
            Integer change = cr.getChangeAmount();
            if (change == null || change <= 0) continue;

            boolean projectLike = isProjectLike(cr.getRelatedType(), cr.getReason());
            if (projectLike) {
                projectsCompleted++;
            }
        }
        // 如果没有 “PROJECT” 相关记录，退化为“正向信用变动次数”
        if (projectsCompleted == 0) {
            for (CreditRecord cr : creditRecords) {
                if (cr == null) continue;
                Integer change = cr.getChangeAmount();
                if (change != null && change > 0) {
                    projectsCompleted++;
                }
            }
        }

        // 4) disputes：从 violation_record 映射 verdict
        List<CreditProfileResponse.Dispute> disputes = new ArrayList<>();
        int verifiedDisputeCount = 0;
        for (ViolationRecord vr : violationRecords) {
            if (vr == null) continue;

            String verdict = mapVerdict(vr.getStatus());
            if ("verified".equals(verdict)) verifiedDisputeCount++;

            disputes.add(
                    CreditProfileResponse.Dispute.builder()
                            .id(String.valueOf(vr.getId()))
                            .projectName(StringUtils.hasText(vr.getViolationType()) ? vr.getViolationType() : "Project")
                            .filedBy(vr.getReporterId() != null ? String.valueOf(vr.getReporterId()) : "")
                            .reason(vr.getDescription())
                            .verdict(verdict)
                            .date(formatDate(vr.getReviewedAt() != null ? vr.getReviewedAt() : vr.getCreateTime()))
                            .build()
            );
        }

        // 取最近的 disputes（前端只展示少量）
        disputes.sort(Comparator.comparing(CreditProfileResponse.Dispute::getDate).reversed());
        if (disputes.size() > 6) disputes = disputes.subList(0, 6);

        // 5) reviews：从 credit_record 映射为评分/评论（近似）
        List<CreditProfileResponse.Review> reviews = new ArrayList<>();
        for (CreditRecord cr : creditRecords) {
            if (cr == null) continue;
            if (reviews.size() >= 6) break;
            reviews.add(
                    CreditProfileResponse.Review.builder()
                            .id(String.valueOf(cr.getId()))
                            .projectName(StringUtils.hasText(cr.getRelatedType()) ? cr.getRelatedType() : "Project")
                            .rating(mapRating(cr.getChangeAmount()))
                            .comment(cr.getReason())
                            .from(cr.getOperatorId() != null ? String.valueOf(cr.getOperatorId()) : "")
                            .date(formatDate(cr.getCreateTime()))
                            .build()
            );
        }

        // 6) successRate：按 “成功项目 / 完成项目” 估算（verified disputes 视为扣分）
        int disputeCount = verifiedDisputeCount;
        int successRate;
        if (projectsCompleted <= 0) {
            successRate = 0;
        } else {
            double ok = Math.max(0, projectsCompleted - disputeCount);
            successRate = (int) Math.max(0, Math.min(100, Math.round((ok * 100.0d) / projectsCompleted)));
        }

        return CreditProfileResponse.builder()
                .creditScore(creditScore)
                .projectsCompleted(projectsCompleted)
                .successRate(successRate)
                .disputeCount(disputeCount)
                .disputes(disputes)
                .reviews(reviews)
                .build();
    }

    private int resolveProfileCreditScore(Long subjectId, String subjectType) {
        if ("USER".equals(subjectType)) {
            UserProfile p = userProfileMapper.selectById(subjectId);
            return p != null && p.getCreditScore() != null ? p.getCreditScore() : 0;
        }
        if ("AGENT".equals(subjectType)) {
            AgentProfile p = agentProfileMapper.selectById(subjectId);
            return p != null && p.getCreditScore() != null ? p.getCreditScore() : 0;
        }
        return 0;
    }

    private static boolean isProjectLike(String relatedType, String reason) {
        String rt = relatedType == null ? "" : relatedType.trim().toUpperCase();
        if (rt.contains("PROJECT")) return true;
        if (reason == null) return false;
        return reason.toUpperCase().contains("PROJECT");
    }

    private static String mapVerdict(ViolationRecord.ViolationStatus status) {
        if (status == null) return "unverified";
        return switch (status) {
            case CONFIRMED -> "verified";
            case PENDING -> "pending";
            case REJECTED, APPEALED -> "unverified";
        };
    }

    private static int mapRating(Integer changeAmount) {
        int v = changeAmount != null ? changeAmount : 0;
        if (v >= 100) return 5;
        if (v >= 60) return 4;
        if (v >= 20) return 3;
        if (v > 0) return 2;
        if (v == 0) return 2;
        return 1;
    }

    private static String formatDate(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.toLocalDate().toString();
    }
}

