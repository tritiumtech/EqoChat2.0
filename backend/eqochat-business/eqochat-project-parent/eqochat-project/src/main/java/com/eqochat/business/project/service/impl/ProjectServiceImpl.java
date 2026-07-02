package com.eqochat.business.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectStatus;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.model.WalletCapability;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.actor.api.service.WalletPolicyApi;
import com.eqochat.business.project.api.dto.request.CreateProjectRequest;
import com.eqochat.business.project.api.dto.request.CreateProjectPaymentRequest;
import com.eqochat.business.project.api.dto.request.CreateProjectTaskRequest;
import com.eqochat.business.project.api.dto.request.TransferProjectOwnershipRequest;
import com.eqochat.business.project.api.dto.request.UpdateProjectBidRequest;
import com.eqochat.business.project.api.dto.response.*;
import com.eqochat.business.project.api.service.ProjectService;
import com.eqochat.business.project.config.ProjectModuleProperties;
import com.eqochat.business.project.entity.Project;
import com.eqochat.business.project.entity.ProjectFile;
import com.eqochat.business.project.entity.ProjectMember;
import com.eqochat.business.project.entity.ProjectPayment;
import com.eqochat.business.project.entity.ProjectTask;
import com.eqochat.business.project.mapper.ProjectFileMapper;
import com.eqochat.business.project.mapper.ProjectMapper;
import com.eqochat.business.project.mapper.ProjectMemberMapper;
import com.eqochat.business.project.mapper.ProjectPaymentMapper;
import com.eqochat.business.project.mapper.ProjectTaskMapper;
import com.eqochat.framework.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectTaskMapper projectTaskMapper;
    private final ProjectPaymentMapper projectPaymentMapper;
    private final ProjectFileMapper projectFileMapper;

    private final SubjectDirectoryApi subjectDirectoryApi;
    private final WalletPolicyApi walletPolicyApi;
    private final LiabilityPolicyApi liabilityPolicyApi;

    private final ProjectModuleProperties properties;

    private final ObjectMapper objectMapper;

    private static final List<String> COLOR_POOL = List.of(
            "#7C3AED", "#D97706", "#DC2626", "#6B7280", "#0EA5E9", "#14B8A6", "#10B981"
    );

    @Override
    public List<ProjectSummaryResponse> listMyProjects(
            Long principalHumanId,
            Long viewerSubjectId,
            SubjectType viewerSubjectType
    ) {
        SubjectRef viewer = requireViewerSubject(principalHumanId, viewerSubjectId, viewerSubjectType);

        Set<Long> projectIds = new LinkedHashSet<>();

        List<ProjectMember> subjectMembers = projectMemberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getMemberId, viewer.id())
                .eq(ProjectMember::getMemberType, toMemberType(viewer.type())));
        addProjectIds(projectIds, subjectMembers);

        List<Project> ownedProjects = projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .eq(Project::getOwnerId, viewer.id())
                .eq(Project::getOwnerType, toProjectOwnerType(viewer.type())));
        if (ownedProjects != null) {
            for (Project p : ownedProjects) {
                if (p != null && p.getId() != null) {
                    projectIds.add(p.getId());
                }
            }
        }

        if (projectIds.isEmpty()) {
            return List.of();
        }

        List<Project> projects = projectMapper.selectBatchIds(projectIds);
        if (projects == null || projects.isEmpty()) return List.of();

        List<ProjectMember> allMembers = projectMemberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                .in(ProjectMember::getProjectId, projectIds));

        Map<Long, List<ProjectMember>> membersByProjectId = new HashMap<>();
        for (ProjectMember member : allMembers) {
            if (member == null || member.getProjectId() == null) continue;
            membersByProjectId.computeIfAbsent(member.getProjectId(), k -> new ArrayList<>()).add(member);
        }

        return projects.stream()
                .map(p -> toSummary(p, membersByProjectId.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    private static void addProjectIds(Set<Long> projectIds, List<ProjectMember> members) {
        if (members == null) {
            return;
        }
        for (ProjectMember m : members) {
            if (m != null && m.getProjectId() != null) {
                projectIds.add(m.getProjectId());
            }
        }
    }

    @Override
    public ProjectDetailResponse createProject(Long principalHumanId, CreateProjectRequest request) {
        if (principalHumanId == null) {
            throw BizException.of("auth.user.not_found");
        }
        if (request == null || !StringUtils.hasText(request.getName()) || request.getBid() == null) {
            throw BizException.of("project.create.invalid");
        }

        SubjectRef owner = requireBusinessSubject(
                request.getOwnerSubjectId(),
                request.getOwnerSubjectType(),
                "project.owner.invalid"
        );
        SubjectSummaryResponse ownerSummary = requireActiveSubject(owner);
        LiabilityChain ownerLiability = requireAuthorizedLiability(principalHumanId, owner, "project.owner.forbidden");

        long bid = request.getBid();
        boolean depositPaid = bid < 100;

        Project project = Project.builder()
                .name(request.getName().trim())
                .status(Project.ProjectStatus.ACTIVE)
                .color(pickColor(request.getName()))
                .revenue("$0")
                .bid(bid)
                .depositPaid(depositPaid)
                .deadline("")
                .progress(0)
                .ownerId(owner.id())
                .ownerType(toProjectOwnerType(owner.type()))
                .agentOwnerMasterId(owner.type() == SubjectType.AGENT ? ownerLiability.liableHumanId() : null)
                .agentFullyAuthorized(owner.type() == SubjectType.AGENT)
                .pendingAgentDecisions(null)
                .pendingBidUpdate(null)
                .pendingOwnershipTransfer(null)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .createBy(principalHumanId)
                .updateBy(principalHumanId)
                .build();

        projectMapper.insert(project);
        if (project.getId() == null) {
            throw BizException.of("error.system");
        }

        // 创建者自动加入项目成员
        ProjectMember ownerMember = memberFromSubject(project.getId(), owner, ownerSummary, ownerLiability, principalHumanId);
        projectMemberMapper.insert(ownerMember);

        return getProjectDetail(principalHumanId, project.getId(), owner.id(), owner.type());
    }

    private ProjectMember memberFromSubject(
            Long projectId,
            SubjectRef member,
            SubjectSummaryResponse summary,
            LiabilityChain liability,
            Long auditHumanId
    ) {
        return ProjectMember.builder()
                .projectId(projectId)
                .memberId(member.id())
                .memberType(toMemberType(member.type()))
                .name(displayName(summary, member))
                .avatarUrl(summary != null ? summary.getAvatarUrl() : null)
                .isOnline(summary != null && summary.getStatus() == SubjectStatus.ACTIVE)
                .masterId(member.type() == SubjectType.AGENT ? liableHumanId(liability) : null)
                .creditScore(summary != null ? summary.getCreditScore() : null)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .createBy(auditHumanId)
                .updateBy(auditHumanId)
                .build();
    }

    @Override
    public ProjectDetailResponse getProjectDetail(
            Long principalHumanId,
            Long projectId,
            Long viewerSubjectId,
            SubjectType viewerSubjectType
    ) {
        if (projectId == null) {
            throw BizException.of("project.required");
        }

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BizException.of("project.not_found");
        }

        SubjectRef viewer = requireViewerSubject(principalHumanId, viewerSubjectId, viewerSubjectType);
        ensureViewerCanAccess(principalHumanId, viewer, projectId, project);

        List<ProjectMember> members = projectMemberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId));

        int humans = 0;
        int agents = 0;
        for (ProjectMember m : members) {
            if (m == null || m.getMemberType() == null) continue;
            if (m.getMemberType() == ProjectMember.MemberType.HUMAN) humans++;
            if (m.getMemberType() == ProjectMember.MemberType.AGENT) agents++;
        }

        List<ProjectTask> tasks = projectTaskMapper.selectList(new LambdaQueryWrapper<ProjectTask>()
                .eq(ProjectTask::getProjectId, projectId));
        List<ProjectPayment> payments = projectPaymentMapper.selectList(new LambdaQueryWrapper<ProjectPayment>()
                .eq(ProjectPayment::getProjectId, projectId));

        int tasksTotal = tasks == null ? 0 : tasks.size();
        int tasksCompleted = 0;
        long earned = 0;
        long pending = 0;

        if (tasks != null) {
            for (ProjectTask t : tasks) {
                if (t != null && t.getStatus() == ProjectTask.TaskStatus.COMPLETED) {
                    tasksCompleted++;
                }
            }
        }

        if (payments != null) {
            for (ProjectPayment p : payments) {
                if (p == null || p.getAmount() == null) continue;
                if (p.getStatus() == ProjectPayment.PaymentStatus.PAID) {
                    earned += p.getAmount();
                } else {
                    pending += p.getAmount();
                }
            }
        }

        String efficiency = computeEfficiency(tasksCompleted, tasksTotal);

        ProjectDetailResponse.PendingBidUpdate pendingBidUpdate = parsePendingBidUpdate(
                project,
                members == null ? 0 : members.size()
        );
        SubjectRef owner = ownerRef(project);
        SubjectSummaryResponse ownerSummary = subjectDirectoryApi.getSubject(owner);
        LiabilityChain ownerLiability = liabilityPolicyApi.resolveLiability(owner);

        return ProjectDetailResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .status(toLower(project.getStatus() != null ? project.getStatus().name() : null))
                .color(project.getColor())
                .humans(humans)
                .agents(agents)
                .revenue(project.getRevenue())
                .bid(project.getBid())
                .depositPaid(Boolean.TRUE.equals(project.getDepositPaid()))
                .deadline(project.getDeadline())
                .progress(project.getProgress() != null ? project.getProgress() : 0)
                .ownerSubjectId(owner.id())
                .ownerSubjectType(owner.type())
                .ownerDisplayName(displayName(ownerSummary, owner))
                .associatedHumanId(owner.type() == SubjectType.AGENT
                        ? associatedHumanId(ownerSummary, ownerLiability)
                        : null)
                .associatedHumanName(owner.type() == SubjectType.AGENT ? associatedHumanName(ownerSummary) : null)
                .liableHumanId(liableHumanId(ownerLiability))
                .agentFullyAuthorized(project.getOwnerType() == Project.ProjectOwnerType.AGENT
                        && Boolean.TRUE.equals(project.getAgentFullyAuthorized()))
                .walletRouting(walletRouting(project))
                .responsibilityChain(responsibilityChain(project))
                .members(members == null ? List.of() : members.stream().map(this::toMemberResponse).toList())
                .pendingBidUpdate(pendingBidUpdate)
                .stats(ProjectDetailResponse.ProjectStats.builder()
                        .earned(formatMoney(earned))
                        .pending(formatMoney(pending))
                        .tasksCompleted(tasksCompleted)
                        .tasksTotal(tasksTotal)
                        .efficiency(efficiency)
                        .build())
                .build();
    }

    @Override
    public void requestBidUpdate(Long principalHumanId, Long projectId, UpdateProjectBidRequest request) {
        if (principalHumanId == null) {
            throw BizException.of("auth.user.not_found");
        }
        if (projectId == null) {
            throw BizException.of("project.required");
        }
        if (request == null || request.getNewBid() == null) {
            throw BizException.of("project.bid.required");
        }

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BizException.of("project.not_found");
        }
        SubjectRef actor = requireActorSubject(
                principalHumanId,
                request.getActorSubjectId(),
                request.getActorSubjectType(),
                "project.bid.actor.invalid",
                "project.bid.forbidden"
        );
        ensureActorCanAccessProject(principalHumanId, actor, projectId, project);

        requireCurrentOwnerAuthority(principalHumanId, actor, project, "project.bid.forbidden");

        long newBid = request.getNewBid();
        project.setBid(newBid);
        project.setDepositPaid(newBid < 100);
        project.setPendingBidUpdate(null);
        project.setUpdateBy(principalHumanId);
        project.setUpdateTime(LocalDateTime.now());

        projectMapper.updateById(project);
    }

    @Override
    public void transferOwnership(Long principalHumanId, Long projectId, TransferProjectOwnershipRequest request) {
        if (principalHumanId == null) {
            throw BizException.of("auth.user.not_found");
        }
        if (projectId == null) {
            throw BizException.of("project.required");
        }
        if (request == null || request.getNewOwnerSubjectId() == null || request.getNewOwnerSubjectType() == null) {
            throw BizException.of("project.transfer.invalid");
        }

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BizException.of("project.not_found");
        }
        SubjectRef actor = requireActorSubject(
                principalHumanId,
                request.getActorSubjectId(),
                request.getActorSubjectType(),
                "project.transfer.actor.invalid",
                "project.transfer.forbidden"
        );
        ensureActorCanAccessProject(principalHumanId, actor, projectId, project);

        requireCurrentOwnerAuthority(principalHumanId, actor, project, "project.transfer.forbidden");
        SubjectRef newOwner = requireBusinessSubject(
                request.getNewOwnerSubjectId(),
                request.getNewOwnerSubjectType(),
                "project.transfer.invalid"
        );
        LiabilityChain newOwnerLiability = newOwner.type() == SubjectType.AGENT
                ? requireAuthorizedLiability(principalHumanId, newOwner, "project.transfer.forbidden")
                : liabilityPolicyApi.resolveLiability(newOwner);

        ProjectMember targetMember = projectMemberMapper.selectOne(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getMemberId, newOwner.id())
                .eq(ProjectMember::getMemberType, toMemberType(newOwner.type())));
        if (targetMember == null) {
            throw BizException.of("project.member.not_found");
        }

        project.setOwnerId(targetMember.getMemberId());
        project.setOwnerType(toProjectOwnerType(newOwner.type()));
        if (newOwner.type() == SubjectType.AGENT) {
            project.setAgentOwnerMasterId(newOwnerLiability.liableHumanId());
            project.setAgentFullyAuthorized(true);
        } else {
            project.setAgentOwnerMasterId(null);
            project.setAgentFullyAuthorized(false);
        }

        project.setPendingOwnershipTransfer(null);
        project.setUpdateBy(principalHumanId);
        project.setUpdateTime(LocalDateTime.now());

        projectMapper.updateById(project);
    }

    @Override
    public ProjectShareLinkResponse shareLink(
            Long principalHumanId,
            Long projectId,
            Long viewerSubjectId,
            SubjectType viewerSubjectType
    ) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BizException.of("project.not_found");
        }
        SubjectRef viewer = requireViewerSubject(principalHumanId, viewerSubjectId, viewerSubjectType);
        ensureViewerCanAccess(principalHumanId, viewer, projectId, project);

        String tpl = properties.getShareUrlTemplate();
        if (!StringUtils.hasText(tpl) || !tpl.contains("{projectId}")) {
            throw BizException.of("project.share_url_template.invalid");
        }
        String url = tpl.replace("{projectId}", String.valueOf(projectId));
        return ProjectShareLinkResponse.builder().url(url).build();
    }

    @Override
    public List<ProjectTaskResponse> listSidebarTasks(
            Long principalHumanId,
            Long projectId,
            Long viewerSubjectId,
            SubjectType viewerSubjectType
    ) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BizException.of("project.not_found");
        }
        SubjectRef viewer = requireViewerSubject(principalHumanId, viewerSubjectId, viewerSubjectType);
        ensureViewerCanAccess(principalHumanId, viewer, projectId, project);

        List<ProjectTask> tasks = projectTaskMapper.selectList(new LambdaQueryWrapper<ProjectTask>()
                .eq(ProjectTask::getProjectId, projectId));
        if (tasks == null) return List.of();

        return tasks.stream().map(this::toTaskResponse)
                .toList();
    }

    @Override
    public List<ProjectPaymentResponse> listSidebarPayments(
            Long principalHumanId,
            Long projectId,
            Long viewerSubjectId,
            SubjectType viewerSubjectType
    ) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BizException.of("project.not_found");
        }
        SubjectRef viewer = requireViewerSubject(principalHumanId, viewerSubjectId, viewerSubjectType);
        ensureViewerCanAccess(principalHumanId, viewer, projectId, project);

        List<ProjectPayment> payments = projectPaymentMapper.selectList(new LambdaQueryWrapper<ProjectPayment>()
                .eq(ProjectPayment::getProjectId, projectId));
        if (payments == null) return List.of();

        return payments.stream().map(this::toPaymentResponse)
                .toList();
    }

    @Override
    public List<ProjectFileResponse> listSidebarFiles(
            Long principalHumanId,
            Long projectId,
            Long viewerSubjectId,
            SubjectType viewerSubjectType
    ) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BizException.of("project.not_found");
        }
        SubjectRef viewer = requireViewerSubject(principalHumanId, viewerSubjectId, viewerSubjectType);
        ensureViewerCanAccess(principalHumanId, viewer, projectId, project);

        List<ProjectFile> files = projectFileMapper.selectList(new LambdaQueryWrapper<ProjectFile>()
                .eq(ProjectFile::getProjectId, projectId));
        if (files == null) return List.of();

        return files.stream().map(this::toFileResponse)
                .toList();
    }

    private void ensureViewerCanAccess(Long principalHumanId, SubjectRef viewer, Long projectId, Project project) {
        if (project == null) {
            throw BizException.of("project.not_found");
        }
        if (principalHumanId == null) {
            throw BizException.of("auth.user.not_found");
        }
        if (viewer == null) {
            throw BizException.of("project.viewer.invalid");
        }
        ensureSubjectCanAccess(viewer, projectId, project);
    }

    private void ensureActorCanAccessProject(Long principalHumanId, SubjectRef actor, Long projectId, Project project) {
        if (project == null) {
            throw BizException.of("project.not_found");
        }
        if (principalHumanId == null) {
            throw BizException.of("auth.user.not_found");
        }
        if (actor == null) {
            throw BizException.of("project.forbidden");
        }
        ensureSubjectCanAccess(actor, projectId, project);
    }

    private void ensureSubjectCanAccess(SubjectRef subject, Long projectId, Project project) {
        SubjectRef owner = ownerRef(project);
        if (owner.equals(subject)) {
            return;
        }

        Long memberCount = projectMemberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getMemberId, subject.id())
                .eq(ProjectMember::getMemberType, toMemberType(subject.type())));
        if (memberCount != null && memberCount > 0) {
            return;
        }

        throw BizException.of("project.forbidden");
    }

    private ProjectSummaryResponse toSummary(Project project, List<ProjectMember> members) {
        int humans = 0;
        int agents = 0;
        if (members != null) {
            for (ProjectMember m : members) {
                if (m == null || m.getMemberType() == null) continue;
                if (m.getMemberType() == ProjectMember.MemberType.HUMAN) humans++;
                if (m.getMemberType() == ProjectMember.MemberType.AGENT) agents++;
            }
        }
        SubjectRef owner = ownerRef(project);
        SubjectSummaryResponse ownerSummary = subjectDirectoryApi.getSubject(owner);
        LiabilityChain ownerLiability = liabilityPolicyApi.resolveLiability(owner);

        return ProjectSummaryResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .status(toLower(project.getStatus() != null ? project.getStatus().name() : null))
                .color(project.getColor())
                .humans(humans)
                .agents(agents)
                .revenue(project.getRevenue())
                .bid(project.getBid())
                .depositPaid(Boolean.TRUE.equals(project.getDepositPaid()))
                .deadline(project.getDeadline())
                .progress(project.getProgress() != null ? project.getProgress() : 0)
                .ownerSubjectId(owner.id())
                .ownerSubjectType(owner.type())
                .ownerDisplayName(displayName(ownerSummary, owner))
                .associatedHumanId(owner.type() == SubjectType.AGENT
                        ? associatedHumanId(ownerSummary, ownerLiability)
                        : null)
                .associatedHumanName(owner.type() == SubjectType.AGENT
                        ? associatedHumanName(ownerSummary)
                        : null)
                .liableHumanId(liableHumanId(ownerLiability))
                .agentFullyAuthorized(project.getOwnerType() == Project.ProjectOwnerType.AGENT
                        && Boolean.TRUE.equals(project.getAgentFullyAuthorized()))
                .walletRouting(walletRouting(project))
                .responsibilityChain(responsibilityChain(project))
                .pendingBidUpdate(StringUtils.hasText(project.getPendingBidUpdate()))
                .build();
    }

    private ProjectDetailResponse.PendingBidUpdate parsePendingBidUpdate(Project project, int memberCount) {
        if (project == null) return null;
        String raw = project.getPendingBidUpdate();
        if (!StringUtils.hasText(raw)) return null;
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node == null || node.isNull()) return null;

            long newBid = node.path("newBid").asLong(0L);

            List<String> approvals = new ArrayList<>();
            JsonNode ap = node.path("approvals");
            if (ap.isArray()) {
                for (JsonNode v : ap) {
                    if (v != null && !v.isNull()) approvals.add(v.asText());
                }
            }

            List<String> rejections = new ArrayList<>();
            JsonNode rj = node.path("rejections");
            if (rj.isArray()) {
                for (JsonNode v : rj) {
                    if (v != null && !v.isNull()) rejections.add(v.asText());
                }
            }

            int pending = node.path("pending").isNumber()
                    ? node.path("pending").asInt(0)
                    : Math.max(0, memberCount - approvals.size() - rejections.size());

            return ProjectDetailResponse.PendingBidUpdate.builder()
                    .newBid(newBid)
                    .approvals(approvals)
                    .rejections(rejections)
                    .pending(pending)
                    .build();
        } catch (Exception ignore) {
            // pendingBidUpdate JSON 解析失败时，直接忽略该信息
            return null;
        }
    }

    private ProjectMemberResponse toMemberResponse(ProjectMember member) {
        if (member == null) return null;
        SubjectRef memberRef = memberRef(member);
        SubjectSummaryResponse subject = subjectDirectoryApi.getSubject(memberRef);
        LiabilityChain liability = liabilityPolicyApi.resolveLiability(memberRef);
        return ProjectMemberResponse.builder()
                .memberSubjectId(memberRef.id())
                .memberSubjectType(memberRef.type())
                .name(subject != null ? displayName(subject, memberRef) : member.getName())
                .avatarUrl(subject != null ? subject.getAvatarUrl() : member.getAvatarUrl())
                .isOnline(Boolean.TRUE.equals(member.getIsOnline()))
                .associatedHumanId(memberRef.type() == SubjectType.AGENT
                        ? associatedHumanId(subject, liability, member.getMasterId())
                        : null)
                .associatedHumanName(memberRef.type() == SubjectType.AGENT ? associatedHumanName(subject) : null)
                .liableHumanId(liableHumanId(liability))
                .creditScore(subject != null && subject.getCreditScore() != null
                        ? subject.getCreditScore()
                        : member.getCreditScore())
                .build();
    }

    private static String toLower(String s) {
        if (!StringUtils.hasText(s)) return "";
        return s.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private String walletRouting(Project project) {
        WalletCapability wallet = walletPolicyApi.resolveWallet(project != null ? ownerRef(project) : null);
        return wallet != null ? wallet.routing() : "AGENT_TO_OWNER";
    }

    private String responsibilityChain(Project project) {
        if (project == null) return "";
        SubjectRef owner = ownerRef(project);
        LiabilityChain liability = liabilityPolicyApi.resolveLiability(owner);
        return liability != null ? liability.route() : owner.type().jsonValue() + ":" + owner.id() + "->human:unknown";
    }

    private static String toTaskStatus(ProjectTask.TaskStatus status) {
        if (status == null) return "pending";
        String s = status.name().toLowerCase(Locale.ROOT).replace('_', '-');
        return s;
    }

    private static String toPaymentStatus(ProjectPayment.PaymentStatus status) {
        if (status == null) return "pending";
        return status.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String computeEfficiency(int completed, int total) {
        if (total <= 0) return "+0%";
        int pct = (int) Math.round((completed * 100.0) / total);
        return "+" + pct + "%";
    }

    private static String formatMoney(long amount) {
        if (amount <= 0) return "$0";
        return "$" + amount;
    }

    private String pickColor(String seed) {
        if (!StringUtils.hasText(seed)) return COLOR_POOL.get(0);
        int idx = Math.abs(seed.trim().hashCode()) % COLOR_POOL.size();
        return COLOR_POOL.get(idx);
    }

    private static ProjectTask.TaskPriority parseTaskPriority(String raw) {
        if (!StringUtils.hasText(raw)) {
            return ProjectTask.TaskPriority.MEDIUM;
        }
        String v = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return ProjectTask.TaskPriority.valueOf(v);
        } catch (IllegalArgumentException e) {
            return ProjectTask.TaskPriority.MEDIUM;
        }
    }

    private SubjectRef requireBusinessSubject(Long subjectId, SubjectType subjectType, String errorCode) {
        if (subjectId == null || subjectType == null || subjectType == SubjectType.SYSTEM) {
            throw BizException.of(errorCode);
        }
        return new SubjectRef(subjectId, subjectType);
    }

    private SubjectRef requireViewerSubject(Long principalHumanId, Long subjectId, SubjectType subjectType) {
        SubjectRef subject = requireBusinessSubject(subjectId, subjectType, "project.viewer.invalid");
        requireAuthorizedLiability(principalHumanId, subject, "project.viewer.forbidden");
        return subject;
    }

    private SubjectRef requireActorSubject(
            Long principalHumanId,
            Long subjectId,
            SubjectType subjectType,
            String invalidCode,
            String forbiddenCode
    ) {
        SubjectRef subject = requireBusinessSubject(subjectId, subjectType, invalidCode);
        requireAuthorizedLiability(principalHumanId, subject, forbiddenCode);
        return subject;
    }

    private SubjectSummaryResponse requireActiveSubject(SubjectRef subject) {
        SubjectSummaryResponse summary = subjectDirectoryApi.getSubject(subject);
        if (summary == null) {
            throw BizException.of("project.subject.not_found");
        }
        if (summary.getStatus() != SubjectStatus.ACTIVE) {
            throw BizException.of("project.subject.invalid");
        }
        return summary;
    }

    private LiabilityChain requireAuthorizedLiability(Long principalHumanId, SubjectRef actor, String errorCode) {
        LiabilityChain liability = liabilityPolicyApi.resolveLiability(actor);
        Long liableHumanId = liableHumanId(liability);
        if (liableHumanId == null || !Objects.equals(liableHumanId, principalHumanId)) {
            throw BizException.of(errorCode);
        }
        return liability;
    }

    private LiabilityChain requireCurrentOwnerAuthority(
            Long principalHumanId,
            SubjectRef actor,
            Project project,
            String errorCode
    ) {
        SubjectRef owner = ownerRef(project);
        if (!owner.equals(actor)) {
            throw BizException.of(errorCode);
        }
        return requireAuthorizedLiability(principalHumanId, actor, errorCode);
    }

    private static SubjectRef ownerRef(Project project) {
        return new SubjectRef(project.getOwnerId(), toSubjectType(project.getOwnerType()));
    }

    private static SubjectRef memberRef(ProjectMember member) {
        return new SubjectRef(member.getMemberId(), toSubjectType(member.getMemberType()));
    }

    private static SubjectRef assigneeRef(ProjectTask task) {
        return new SubjectRef(task.getAssigneeId(), toSubjectType(task.getAssigneeType()));
    }

    private static SubjectRef recipientRef(ProjectPayment payment) {
        return new SubjectRef(payment.getRecipientId(), toSubjectType(payment.getRecipientType()));
    }

    private static SubjectRef uploaderRef(ProjectFile file) {
        return new SubjectRef(file.getUploadedById(), toSubjectType(file.getUploadedByType()));
    }

    private static Project.ProjectOwnerType toProjectOwnerType(SubjectType subjectType) {
        if (subjectType == SubjectType.AGENT) {
            return Project.ProjectOwnerType.AGENT;
        }
        if (subjectType == SubjectType.HUMAN) {
            return Project.ProjectOwnerType.HUMAN;
        }
        throw BizException.of("project.subject.invalid");
    }

    private static ProjectMember.MemberType toMemberType(SubjectType subjectType) {
        if (subjectType == SubjectType.AGENT) {
            return ProjectMember.MemberType.AGENT;
        }
        if (subjectType == SubjectType.HUMAN) {
            return ProjectMember.MemberType.HUMAN;
        }
        throw BizException.of("project.subject.invalid");
    }

    private static ProjectTask.AssigneeType toAssigneeType(SubjectType subjectType) {
        if (subjectType == SubjectType.AGENT) {
            return ProjectTask.AssigneeType.AGENT;
        }
        if (subjectType == SubjectType.HUMAN) {
            return ProjectTask.AssigneeType.HUMAN;
        }
        throw BizException.of("project.subject.invalid");
    }

    private static ProjectPayment.RecipientType toRecipientType(SubjectType subjectType) {
        if (subjectType == SubjectType.AGENT) {
            return ProjectPayment.RecipientType.AGENT;
        }
        if (subjectType == SubjectType.HUMAN) {
            return ProjectPayment.RecipientType.HUMAN;
        }
        throw BizException.of("project.subject.invalid");
    }

    private static SubjectType toSubjectType(Project.ProjectOwnerType ownerType) {
        if (ownerType == Project.ProjectOwnerType.AGENT) return SubjectType.AGENT;
        if (ownerType == Project.ProjectOwnerType.HUMAN) return SubjectType.HUMAN;
        throw BizException.of("project.subject.invalid");
    }

    private static SubjectType toSubjectType(ProjectMember.MemberType memberType) {
        if (memberType == ProjectMember.MemberType.AGENT) return SubjectType.AGENT;
        if (memberType == ProjectMember.MemberType.HUMAN) return SubjectType.HUMAN;
        throw BizException.of("project.subject.invalid");
    }

    private static SubjectType toSubjectType(ProjectTask.AssigneeType assigneeType) {
        if (assigneeType == ProjectTask.AssigneeType.AGENT) return SubjectType.AGENT;
        if (assigneeType == ProjectTask.AssigneeType.HUMAN) return SubjectType.HUMAN;
        throw BizException.of("project.subject.invalid");
    }

    private static SubjectType toSubjectType(ProjectPayment.RecipientType recipientType) {
        if (recipientType == ProjectPayment.RecipientType.AGENT) return SubjectType.AGENT;
        if (recipientType == ProjectPayment.RecipientType.HUMAN) return SubjectType.HUMAN;
        throw BizException.of("project.subject.invalid");
    }

    private static SubjectType toSubjectType(ProjectFile.UploaderType uploaderType) {
        if (uploaderType == ProjectFile.UploaderType.AGENT) return SubjectType.AGENT;
        if (uploaderType == ProjectFile.UploaderType.HUMAN) return SubjectType.HUMAN;
        throw BizException.of("project.subject.invalid");
    }

    private static String displayName(SubjectSummaryResponse subject, SubjectRef fallback) {
        if (subject != null && StringUtils.hasText(subject.getDisplayName())) {
            return subject.getDisplayName();
        }
        return fallback.type().name() + ":" + fallback.id();
    }

    private static Long liableHumanId(LiabilityChain liability) {
        return liability != null ? liability.liableHumanId() : null;
    }

    private static Long associatedHumanId(SubjectSummaryResponse subject, LiabilityChain liability) {
        return associatedHumanId(subject, liability, null);
    }

    private static Long associatedHumanId(SubjectSummaryResponse subject, LiabilityChain liability, Long fallback) {
        if (subject != null && subject.getAssociatedHumanId() != null) {
            return subject.getAssociatedHumanId();
        }
        Long liableHumanId = liableHumanId(liability);
        return liableHumanId != null ? liableHumanId : fallback;
    }

    private static String associatedHumanName(SubjectSummaryResponse subject) {
        return subject != null ? subject.getAssociatedHumanName() : null;
    }

    private record PaymentAuditFacts(
            String walletRouting,
            SubjectRef directRecipient,
            SubjectRef settlementSubject,
            Long settlementHumanId,
            Boolean financialAutonomy,
            String walletPolicyState,
            String walletPolicyReason,
            Long liableHumanId,
            String liabilityRoute,
            String liabilityReason
    ) {
    }

    private ProjectTaskResponse toTaskResponse(ProjectTask task) {
        SubjectRef assignee = assigneeRef(task);
        SubjectSummaryResponse subject = subjectDirectoryApi.getSubject(assignee);
        return ProjectTaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .assigneeSubjectId(assignee.id())
                .assigneeSubjectType(assignee.type())
                .assigneeDisplayName(subject != null ? displayName(subject, assignee) : task.getAssigneeName())
                .deadline(task.getDeadline())
                .status(toTaskStatus(task.getStatus()))
                .priority(toLower(task.getPriority() != null ? task.getPriority().name() : null))
                .build();
    }

    private ProjectPaymentResponse toPaymentResponse(ProjectPayment payment) {
        SubjectRef recipient = recipientRef(payment);
        SubjectSummaryResponse subject = subjectDirectoryApi.getSubject(recipient);
        return ProjectPaymentResponse.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .recipientSubjectId(recipient.id())
                .recipientSubjectType(recipient.type())
                .recipientDisplayName(subject != null ? displayName(subject, recipient) : payment.getRecipientName())
                .masterWallet(payment.getMasterWallet())
                .walletRouting(payment.getWalletRouting())
                .directRecipientSubjectId(payment.getDirectRecipientId())
                .directRecipientSubjectType(payment.getDirectRecipientType())
                .settlementSubjectId(payment.getSettlementSubjectId())
                .settlementSubjectType(payment.getSettlementSubjectType())
                .settlementHumanId(payment.getSettlementHumanId())
                .financialAutonomy(payment.getFinancialAutonomy())
                .walletPolicyState(payment.getWalletPolicyState())
                .walletPolicyReason(payment.getWalletPolicyReason())
                .liableHumanId(payment.getLiableHumanId())
                .liabilityRoute(payment.getLiabilityRoute())
                .liabilityReason(payment.getLiabilityReason())
                .status(toPaymentStatus(payment.getStatus()))
                .date(payment.getDate())
                .build();
    }

    private ProjectFileResponse toFileResponse(ProjectFile file) {
        SubjectRef uploader = uploaderRef(file);
        SubjectSummaryResponse subject = subjectDirectoryApi.getSubject(uploader);
        return ProjectFileResponse.builder()
                .id(file.getId())
                .name(file.getFileName())
                .type(file.getFileType())
                .uploaderSubjectId(uploader.id())
                .uploaderSubjectType(uploader.type())
                .uploaderDisplayName(subject != null ? displayName(subject, uploader) : file.getUploadedByName())
                .size(file.getSize())
                .date(file.getDate())
                .downloadUrl(file.getDownloadUrl())
                .build();
    }

    @Override
    public ProjectTaskResponse createTask(Long principalHumanId, Long projectId, CreateProjectTaskRequest request) {
        if (principalHumanId == null) {
            throw BizException.of("auth.user.not_found");
        }
        if (projectId == null) {
            throw BizException.of("project.required");
        }
        if (request == null || !StringUtils.hasText(request.getTitle())
                || request.getValue() == null || !StringUtils.hasText(request.getDeadline())
                || request.getAssigneeSubjectId() == null || request.getAssigneeSubjectType() == null) {
            throw BizException.of("project.task.create.invalid");
        }

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BizException.of("project.not_found");
        }
        SubjectRef actor = requireActorSubject(
                principalHumanId,
                request.getActorSubjectId(),
                request.getActorSubjectType(),
                "project.task.actor.invalid",
                "project.forbidden"
        );
        ensureActorCanAccessProject(principalHumanId, actor, projectId, project);

        SubjectRef assignee = requireBusinessSubject(
                request.getAssigneeSubjectId(),
                request.getAssigneeSubjectType(),
                "project.task.assignee.invalid"
        );
        ProjectMember assigneeMember = projectMemberMapper.selectOne(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getMemberId, assignee.id())
                .eq(ProjectMember::getMemberType, toMemberType(assignee.type())));
        if (assigneeMember == null) {
            throw BizException.of("project.member.not_found");
        }
        SubjectSummaryResponse assigneeSummary = requireActiveSubject(assignee);

        ProjectTask task = ProjectTask.builder()
                .projectId(projectId)
                .title(request.getTitle().trim())
                .assigneeId(assignee.id())
                .assigneeType(toAssigneeType(assignee.type()))
                .assigneeName(displayName(assigneeSummary, assignee))
                .deadline(request.getDeadline())
                .status(ProjectTask.TaskStatus.PENDING)
                .priority(parseTaskPriority(request.getPriority()))
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .createBy(principalHumanId)
                .updateBy(principalHumanId)
                .build();

        projectTaskMapper.insert(task);

        return toTaskResponse(task);
    }

    @Override
    public ProjectPaymentResponse createPayment(Long principalHumanId, Long projectId, CreateProjectPaymentRequest request) {
        if (principalHumanId == null) {
            throw BizException.of("auth.user.not_found");
        }
        if (projectId == null) {
            throw BizException.of("project.required");
        }
        if (request == null || request.getAmount() == null || request.getAmount() <= 0
                || request.getRecipientSubjectId() == null || request.getRecipientSubjectType() == null) {
            throw BizException.of("project.payment.create.invalid");
        }

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BizException.of("project.not_found");
        }
        SubjectRef actor = requireActorSubject(
                principalHumanId,
                request.getActorSubjectId(),
                request.getActorSubjectType(),
                "project.payment.actor.invalid",
                "project.forbidden"
        );
        ensureActorCanAccessProject(principalHumanId, actor, projectId, project);

        SubjectRef recipient = requireBusinessSubject(
                request.getRecipientSubjectId(),
                request.getRecipientSubjectType(),
                "project.payment.recipient.invalid"
        );
        ProjectMember recipientMember = projectMemberMapper.selectOne(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getMemberId, recipient.id())
                .eq(ProjectMember::getMemberType, toMemberType(recipient.type())));
        if (recipientMember == null) {
            throw BizException.of("project.member.not_found");
        }

        SubjectSummaryResponse recipientSummary = requireActiveSubject(recipient);
        PaymentAuditFacts facts = resolvePaymentAuditFacts(recipient);
        String walletRouting = StringUtils.hasText(facts.walletRouting()) ? facts.walletRouting() : "NONE";

        ProjectPayment payment = ProjectPayment.builder()
                .projectId(projectId)
                .amount(request.getAmount())
                .recipientId(recipient.id())
                .recipientType(toRecipientType(recipient.type()))
                .recipientName(displayName(recipientSummary, recipient))
                .masterWallet(walletRouting)
                .walletRouting(walletRouting)
                .directRecipientId(facts.directRecipient().id())
                .directRecipientType(facts.directRecipient().type())
                .settlementSubjectId(facts.settlementSubject().id())
                .settlementSubjectType(facts.settlementSubject().type())
                .settlementHumanId(facts.settlementHumanId())
                .financialAutonomy(Boolean.TRUE.equals(facts.financialAutonomy()))
                .walletPolicyState(facts.walletPolicyState())
                .walletPolicyReason(facts.walletPolicyReason())
                .liableHumanId(facts.liableHumanId())
                .liabilityRoute(facts.liabilityRoute())
                .liabilityReason(facts.liabilityReason())
                .status(parsePaymentStatus(request.getStatus()))
                .date(StringUtils.hasText(request.getDate()) ? request.getDate().trim() : LocalDate.now().toString())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .createBy(principalHumanId)
                .updateBy(principalHumanId)
                .build();

        projectPaymentMapper.insert(payment);

        return toPaymentResponse(payment);
    }

    private PaymentAuditFacts resolvePaymentAuditFacts(SubjectRef recipient) {
        WalletCapability wallet = walletPolicyApi.resolveWallet(recipient);
        LiabilityChain liability = liabilityPolicyApi.resolveLiability(recipient);
        Long liableHumanId = liableHumanId(liability);
        if (liableHumanId == null) {
            throw BizException.of("project.payment.liability.invalid");
        }
        if (wallet == null) {
            throw BizException.of("project.payment.wallet.invalid");
        }

        SubjectRef directRecipient = wallet.directRecipient() != null ? wallet.directRecipient() : recipient;
        if (directRecipient.id() == null || directRecipient.type() == SubjectType.SYSTEM) {
            throw BizException.of("project.payment.wallet.invalid");
        }

        SubjectRef settlementSubject = wallet.settlementSubject();
        if (settlementSubject == null || settlementSubject.id() == null || settlementSubject.type() == SubjectType.SYSTEM) {
            throw BizException.of("project.payment.wallet.invalid");
        }

        return new PaymentAuditFacts(
                wallet.routing(),
                directRecipient,
                settlementSubject,
                wallet.settlementHumanId(),
                Boolean.TRUE.equals(wallet.financialAutonomy()),
                wallet.state() != null ? wallet.state().name() : null,
                wallet.reason(),
                liableHumanId,
                liability != null ? liability.route() : null,
                liability != null ? liability.reason() : null
        );
    }

    private static ProjectPayment.PaymentStatus parsePaymentStatus(String raw) {
        if (!StringUtils.hasText(raw)) {
            return ProjectPayment.PaymentStatus.PENDING;
        }
        String v = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return ProjectPayment.PaymentStatus.valueOf(v);
        } catch (IllegalArgumentException e) {
            throw BizException.of("project.payment.status.invalid");
        }
    }
}
