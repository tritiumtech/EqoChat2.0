package com.eqochat.business.credit.service.impl;

import com.eqochat.business.credit.api.dto.response.CreditProfileResponse;
import com.eqochat.business.credit.api.service.CreditProfileService;
import com.eqochat.business.credit.entity.CreditRecord;
import com.eqochat.business.credit.entity.ViolationRecord;
import com.eqochat.business.credit.mapper.CreditRecordMapper;
import com.eqochat.business.credit.mapper.SubjectCreditProfileMapper;
import com.eqochat.business.credit.mapper.ViolationRecordMapper;
import com.eqochat.framework.common.BizException;
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
    private final SubjectCreditProfileMapper subjectCreditProfileMapper;

    @Override
    public CreditProfileResponse getSubjectCreditProfile(Long subjectId, String subjectType) {
        CreditRecord.SubjectType st = parseSubjectType(subjectType);
        String subjectTypeValue = st.name();

        SubjectCreditProfileMapper.SubjectCreditProfileRow profile =
                subjectCreditProfileMapper.selectProfileBySubject(subjectId, subjectTypeValue);
        List<CreditRecord> creditRecords = creditRecordMapper.findBySubject(subjectId, subjectTypeValue);
        List<ViolationRecord> violationRecords = violationRecordMapper.findBySubject(subjectId, subjectTypeValue);

        int projectsCompleted = estimateProjectsCompleted(creditRecords);
        List<CreditProfileResponse.Dispute> disputes = toDisputes(violationRecords);
        int verifiedDisputeCount = verifiedDisputeCount(violationRecords);
        List<CreditProfileResponse.Review> reviews = toReviews(creditRecords);

        int creditScore = resolveCreditScore(profile, creditRecords);
        int disputeCount = profile != null
                ? valueOrDefault(profile.getDisputeCount(), verifiedDisputeCount)
                : verifiedDisputeCount;
        int resolvedProjectsCompleted = profile != null
                ? valueOrDefault(profile.getProjectsCompleted(), projectsCompleted)
                : projectsCompleted;
        int successRate = profile != null && profile.getSuccessRate() != null
                ? clamp(profile.getSuccessRate(), 0, 100)
                : estimateSuccessRate(resolvedProjectsCompleted, disputeCount);

        return CreditProfileResponse.builder()
                .creditScore(creditScore)
                .projectsCompleted(resolvedProjectsCompleted)
                .successRate(successRate)
                .disputeCount(disputeCount)
                .disputes(disputes)
                .reviews(reviews)
                .build();
    }

    private static int resolveCreditScore(
            SubjectCreditProfileMapper.SubjectCreditProfileRow profile,
            List<CreditRecord> creditRecords
    ) {
        if (profile != null && profile.getScore() != null) {
            return adaptCreditScore(profile.getScore());
        }
        if (creditRecords != null && !creditRecords.isEmpty() && creditRecords.get(0).getCurrentScore() != null) {
            return adaptCreditScore(creditRecords.get(0).getCurrentScore());
        }
        return 300;
    }

    private static int estimateProjectsCompleted(List<CreditRecord> creditRecords) {
        int projectLikeCount = 0;
        int positiveCount = 0;
        for (CreditRecord cr : safe(creditRecords)) {
            Integer change = cr.getChangeAmount();
            if (change == null || change <= 0) {
                continue;
            }
            positiveCount++;
            if (isProjectLike(cr.getRelatedType(), cr.getReason())) {
                projectLikeCount++;
            }
        }
        return projectLikeCount > 0 ? projectLikeCount : positiveCount;
    }

    private static List<CreditProfileResponse.Dispute> toDisputes(List<ViolationRecord> violationRecords) {
        List<CreditProfileResponse.Dispute> disputes = new ArrayList<>();
        for (ViolationRecord vr : safe(violationRecords)) {
            disputes.add(
                    CreditProfileResponse.Dispute.builder()
                            .id(String.valueOf(vr.getId()))
                            .projectName(StringUtils.hasText(vr.getViolationType()) ? vr.getViolationType() : "Project")
                            .filedBy(vr.getReporterId() != null ? String.valueOf(vr.getReporterId()) : "")
                            .reason(vr.getDescription())
                            .verdict(mapVerdict(vr.getStatus()))
                            .date(formatDate(vr.getReviewedAt() != null ? vr.getReviewedAt() : vr.getCreateTime()))
                            .build()
            );
        }
        disputes.sort(Comparator.comparing(CreditProfileResponse.Dispute::getDate).reversed());
        return disputes.size() > 6 ? disputes.subList(0, 6) : disputes;
    }

    private static int verifiedDisputeCount(List<ViolationRecord> violationRecords) {
        int count = 0;
        for (ViolationRecord vr : safe(violationRecords)) {
            if ("verified".equals(mapVerdict(vr.getStatus()))) {
                count++;
            }
        }
        return count;
    }

    private static List<CreditProfileResponse.Review> toReviews(List<CreditRecord> creditRecords) {
        List<CreditProfileResponse.Review> reviews = new ArrayList<>();
        for (CreditRecord cr : safe(creditRecords)) {
            if (reviews.size() >= 6) {
                break;
            }
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
        return reviews;
    }

    private static CreditRecord.SubjectType parseSubjectType(String subjectType) {
        try {
            return CreditRecord.SubjectType.valueOf(subjectType == null ? "" : subjectType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw BizException.of("error.invalid.subject_type");
        }
    }

    private static boolean isProjectLike(String relatedType, String reason) {
        String rt = relatedType == null ? "" : relatedType.trim().toUpperCase();
        if (rt.contains("PROJECT")) {
            return true;
        }
        return reason != null && reason.toUpperCase().contains("PROJECT");
    }

    private static String mapVerdict(ViolationRecord.ViolationStatus status) {
        if (status == null) {
            return "unverified";
        }
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
        if (v >= 0) return 2;
        return 1;
    }

    private static int adaptCreditScore(Integer score) {
        if (score == null) {
            return 300;
        }
        if (score >= 300 && score <= 850) {
            return score;
        }
        if (score >= 0 && score <= 100) {
            return clamp(300 + Math.round(score * 5.5f), 300, 850);
        }
        return clamp(score, 300, 850);
    }

    private static int estimateSuccessRate(int projectsCompleted, int disputeCount) {
        if (projectsCompleted <= 0) {
            return 0;
        }
        double ok = Math.max(0, projectsCompleted - disputeCount);
        return clamp((int) Math.round((ok * 100.0d) / projectsCompleted), 0, 100);
    }

    private static int valueOrDefault(Integer value, int fallback) {
        return value != null ? Math.max(0, value) : fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String formatDate(LocalDateTime dt) {
        return dt == null ? "" : dt.toLocalDate().toString();
    }

    private static <T> List<T> safe(List<T> items) {
        return items == null ? List.of() : items;
    }
}
