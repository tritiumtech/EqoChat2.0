package com.eqochat.project.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eqochat.common.BizException;
import com.eqochat.config.ProjectModuleProperties;
import com.eqochat.dto.request.CreateProjectRequest;
import com.eqochat.dto.request.TransferProjectOwnershipRequest;
import com.eqochat.dto.request.UpdateProjectBidRequest;
import com.eqochat.dto.response.*;
import com.eqochat.domain.entity.*;
import com.eqochat.mapper.*;
import com.eqochat.project.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private final com.eqochat.mapper.UserProfileMapper userProfileMapper;
    private final AgentProfileMapper agentProfileMapper;

    private final ProjectModuleProperties properties;

    private final ObjectMapper objectMapper;

    private static final List<String> COLOR_POOL = List.of(
            "#7C3AED", "#D97706", "#DC2626", "#6B7280", "#0EA5E9", "#14B8A6", "#10B981"
    );

    @Override
    public List<ProjectSummaryResponse> listMyProjects(Long viewerId) {
        if (viewerId == null) {
            throw BizException.of("auth.user.not_found");
        }

        List<ProjectMember> myMembers = projectMemberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getMemberId, viewerId)
                .eq(ProjectMember::getMemberType, ProjectMember.MemberType.HUMAN));

        if (CollectionUtils.isEmpty(myMembers)) {
            return List.of();
        }

        Set<Long> projectIds = new HashSet<>();
        for (ProjectMember m : myMembers) {
            if (m != null && m.getProjectId() != null) {
                projectIds.add(m.getProjectId());
            }
        }
        if (projectIds.isEmpty()) return List.of();

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

    @Override
    public ProjectDetailResponse createProject(Long viewerId, CreateProjectRequest request) {
        if (viewerId == null) {
            throw BizException.of("auth.user.not_found");
        }
        if (request == null || !StringUtils.hasText(request.getName()) || request.getBid() == null) {
            throw BizException.of("project.create.invalid");
        }

        UserProfile profile = userProfileMapper.selectById(viewerId);
        if (profile == null) {
            throw BizException.of("auth.user.not_found");
        }

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
                .ownerId(viewerId)
                .ownerType(Project.ProjectOwnerType.HUMAN)
                .agentOwnerMasterId(null)
                .agentFullyAuthorized(false)
                .pendingAgentDecisions(null)
                .pendingBidUpdate(null)
                .pendingOwnershipTransfer(null)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .createBy(viewerId)
                .updateBy(viewerId)
                .build();

        projectMapper.insert(project);
        if (project.getId() == null) {
            throw BizException.of("error.system");
        }

        // 创建者自动加入项目成员
        ProjectMember ownerMember = ProjectMember.builder()
                .projectId(project.getId())
                .memberId(viewerId)
                .memberType(ProjectMember.MemberType.HUMAN)
                .name(StringUtils.hasText(profile.getNickname()) ? profile.getNickname() : (profile.getEmail() != null ? profile.getEmail() : "User"))
                .avatarUrl(profile.getAvatarUrl())
                .isOnline(true)
                .masterId(null)
                .creditScore(profile.getCreditScore())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .createBy(viewerId)
                .updateBy(viewerId)
                .build();
        projectMemberMapper.insert(ownerMember);

        return getProjectDetail(viewerId, project.getId());
    }

    @Override
    public ProjectDetailResponse getProjectDetail(Long viewerId, Long projectId) {
        if (projectId == null) {
            throw BizException.of("project.required");
        }

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BizException.of("project.not_found");
        }

        ensureViewerCanAccess(viewerId, projectId, project);

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
                .ownerId(project.getOwnerId())
                .ownerType(project.getOwnerType() == Project.ProjectOwnerType.AGENT ? "agent" : "human")
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
    public void requestBidUpdate(Long viewerId, Long projectId, UpdateProjectBidRequest request) {
        if (viewerId == null) {
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
        ensureViewerCanAccess(viewerId, projectId, project);

        if (project.getOwnerType() != Project.ProjectOwnerType.HUMAN || !Objects.equals(project.getOwnerId(), viewerId)) {
            throw BizException.of("project.bid.forbidden");
        }

        long newBid = request.getNewBid();
        project.setBid(newBid);
        project.setDepositPaid(newBid < 100);
        project.setPendingBidUpdate(null);
        project.setUpdateBy(viewerId);
        project.setUpdateTime(LocalDateTime.now());

        projectMapper.updateById(project);
    }

    @Override
    public void transferOwnership(Long viewerId, Long projectId, TransferProjectOwnershipRequest request) {
        if (viewerId == null) {
            throw BizException.of("auth.user.not_found");
        }
        if (projectId == null) {
            throw BizException.of("project.required");
        }
        if (request == null || request.getToMemberId() == null || !StringUtils.hasText(request.getToMemberType())) {
            throw BizException.of("project.transfer.invalid");
        }

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BizException.of("project.not_found");
        }
        ensureViewerCanAccess(viewerId, projectId, project);

        if (project.getOwnerType() != Project.ProjectOwnerType.HUMAN || !Objects.equals(project.getOwnerId(), viewerId)) {
            throw BizException.of("project.transfer.forbidden");
        }

        ProjectMember.MemberType toType = parseMemberType(request.getToMemberType());

        ProjectMember targetMember = projectMemberMapper.selectOne(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getMemberId, request.getToMemberId())
                .eq(ProjectMember::getMemberType, toType));
        if (targetMember == null) {
            throw BizException.of("project.member.not_found");
        }

        project.setOwnerId(targetMember.getMemberId());
        project.setOwnerType(toType == ProjectMember.MemberType.AGENT ? Project.ProjectOwnerType.AGENT : Project.ProjectOwnerType.HUMAN);
        if (toType == ProjectMember.MemberType.AGENT) {
            AgentProfile agent = agentProfileMapper.selectById(targetMember.getMemberId());
            if (agent != null) {
                project.setAgentOwnerMasterId(agent.getOwnerId());
                project.setAgentFullyAuthorized(true);
            } else {
                // agent 基础信息缺失时，仍允许转让（但标记为非完全授权）
                project.setAgentOwnerMasterId(viewerId);
                project.setAgentFullyAuthorized(false);
            }
        } else {
            project.setAgentOwnerMasterId(null);
            project.setAgentFullyAuthorized(false);
        }

        project.setPendingOwnershipTransfer(null);
        project.setUpdateBy(viewerId);
        project.setUpdateTime(LocalDateTime.now());

        projectMapper.updateById(project);
    }

    @Override
    public ProjectShareLinkResponse shareLink(Long viewerId, Long projectId) {
        ensureViewerCanAccess(viewerId, projectId, projectMapper.selectById(projectId));

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BizException.of("project.not_found");
        }

        String tpl = properties.getShareUrlTemplate();
        if (!StringUtils.hasText(tpl) || !tpl.contains("{projectId}")) {
            tpl = "http://127.0.0.1:5173/#/pages/project/project?projectId={projectId}";
        }
        String url = tpl.replace("{projectId}", String.valueOf(projectId));
        return ProjectShareLinkResponse.builder().url(url).build();
    }

    @Override
    public List<ProjectTaskResponse> listSidebarTasks(Long viewerId, Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BizException.of("project.not_found");
        }
        ensureViewerCanAccess(viewerId, projectId, project);

        List<ProjectTask> tasks = projectTaskMapper.selectList(new LambdaQueryWrapper<ProjectTask>()
                .eq(ProjectTask::getProjectId, projectId));
        if (tasks == null) return List.of();

        return tasks.stream().map(t -> ProjectTaskResponse.builder()
                        .id(t.getId())
                        .title(t.getTitle())
                        .assignee(t.getAssigneeName())
                        .isAgent(t.getAssigneeType() == ProjectTask.AssigneeType.AGENT)
                        .deadline(t.getDeadline())
                        .status(toTaskStatus(t.getStatus()))
                        .priority(toLower(t.getPriority() != null ? t.getPriority().name() : null))
                        .build())
                .toList();
    }

    @Override
    public List<ProjectPaymentResponse> listSidebarPayments(Long viewerId, Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BizException.of("project.not_found");
        }
        ensureViewerCanAccess(viewerId, projectId, project);

        List<ProjectPayment> payments = projectPaymentMapper.selectList(new LambdaQueryWrapper<ProjectPayment>()
                .eq(ProjectPayment::getProjectId, projectId));
        if (payments == null) return List.of();

        return payments.stream().map(p -> ProjectPaymentResponse.builder()
                        .id(p.getId())
                        .amount(p.getAmount())
                        .recipient(p.getRecipientName())
                        .isAgent(p.getRecipientType() == ProjectPayment.RecipientType.AGENT)
                        .masterWallet(p.getMasterWallet())
                        .status(toPaymentStatus(p.getStatus()))
                        .date(p.getDate())
                        .build())
                .toList();
    }

    @Override
    public List<ProjectFileResponse> listSidebarFiles(Long viewerId, Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BizException.of("project.not_found");
        }
        ensureViewerCanAccess(viewerId, projectId, project);

        List<ProjectFile> files = projectFileMapper.selectList(new LambdaQueryWrapper<ProjectFile>()
                .eq(ProjectFile::getProjectId, projectId));
        if (files == null) return List.of();

        return files.stream().map(f -> ProjectFileResponse.builder()
                        .id(f.getId())
                        .name(f.getFileName())
                        .type(f.getFileType())
                        .uploadedBy(f.getUploadedByName())
                        .isAgent(f.getUploadedByType() == ProjectFile.UploaderType.AGENT)
                        .size(f.getSize())
                        .date(f.getDate())
                        .downloadUrl(f.getDownloadUrl())
                        .build())
                .toList();
    }

    private void ensureViewerCanAccess(Long viewerId, Long projectId, Project project) {
        if (project == null) {
            throw BizException.of("project.not_found");
        }
        if (viewerId == null) {
            throw BizException.of("auth.user.not_found");
        }
        if (project.getOwnerType() == Project.ProjectOwnerType.HUMAN && Objects.equals(project.getOwnerId(), viewerId)) {
            return;
        }

        // member access
        Long count = projectMemberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getMemberId, viewerId)
                .eq(ProjectMember::getMemberType, ProjectMember.MemberType.HUMAN));
        if (count != null && count > 0) {
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
                .ownerId(project.getOwnerId())
                .ownerType(project.getOwnerType() == Project.ProjectOwnerType.AGENT ? "agent" : "human")
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
        return ProjectMemberResponse.builder()
                .id(member.getMemberId())
                .name(member.getName())
                .avatarUrl(member.getAvatarUrl())
                .type(member.getMemberType() == ProjectMember.MemberType.AGENT ? "agent" : "human")
                .isOnline(Boolean.TRUE.equals(member.getIsOnline()))
                .masterId(member.getMasterId())
                .creditScore(member.getCreditScore())
                .build();
    }

    private static String toLower(String s) {
        if (!StringUtils.hasText(s)) return "";
        return s.trim().toLowerCase(Locale.ROOT).replace('_', '-');
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

    private static ProjectMember.MemberType parseMemberType(String raw) {
        if (!StringUtils.hasText(raw)) {
            return ProjectMember.MemberType.HUMAN;
        }
        String v = raw.trim().toUpperCase(Locale.ROOT);
        if ("AGENT".equals(v)) return ProjectMember.MemberType.AGENT;
        return ProjectMember.MemberType.HUMAN;
    }
}

