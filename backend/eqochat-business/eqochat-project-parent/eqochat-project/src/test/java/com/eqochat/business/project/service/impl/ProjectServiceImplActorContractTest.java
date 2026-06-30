package com.eqochat.business.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import com.eqochat.business.project.api.dto.response.ProjectDetailResponse;
import com.eqochat.business.project.api.dto.response.ProjectFileResponse;
import com.eqochat.business.project.api.dto.response.ProjectPaymentResponse;
import com.eqochat.business.project.api.dto.response.ProjectTaskResponse;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplActorContractTest {

    @Mock
    ProjectMapper projectMapper;
    @Mock
    ProjectMemberMapper projectMemberMapper;
    @Mock
    ProjectTaskMapper projectTaskMapper;
    @Mock
    ProjectPaymentMapper projectPaymentMapper;
    @Mock
    ProjectFileMapper projectFileMapper;
    @Mock
    SubjectDirectoryApi subjectDirectoryApi;
    @Mock
    WalletPolicyApi walletPolicyApi;
    @Mock
    LiabilityPolicyApi liabilityPolicyApi;

    ProjectServiceImpl service;

    @BeforeEach
    void setUp() {
        initTableInfo(Project.class);
        initTableInfo(ProjectMember.class);
        initTableInfo(ProjectTask.class);
        initTableInfo(ProjectPayment.class);
        initTableInfo(ProjectFile.class);
        service = new ProjectServiceImpl(
                projectMapper,
                projectMemberMapper,
                projectTaskMapper,
                projectPaymentMapper,
                projectFileMapper,
                subjectDirectoryApi,
                walletPolicyApi,
                liabilityPolicyApi,
                new ProjectModuleProperties(),
                new ObjectMapper()
        );
    }

    @Test
    void createProjectPersistsAgentOwnerAndExposesLiableHumanTransparency() {
        AtomicReference<Project> savedProject = new AtomicReference<>();
        AtomicReference<ProjectMember> savedOwnerMember = new AtomicReference<>();

        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Agent Build");
        request.setBid(250L);
        request.setOwnerSubjectId(101L);
        request.setOwnerSubjectType(SubjectType.AGENT);

        SubjectRef agentOwner = SubjectRef.agent(101L);
        when(subjectDirectoryApi.getSubject(agentOwner)).thenReturn(activeSubject(101L, SubjectType.AGENT, "Nova", 9L, "Ava"));
        when(liabilityPolicyApi.resolveLiability(agentOwner)).thenReturn(LiabilityChain.agentToHuman(101L, 9L));
        when(walletPolicyApi.resolveWallet(agentOwner)).thenReturn(WalletCapability.agentToOwner(101L, 9L, "owner settlement"));
        doAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(501L);
            savedProject.set(project);
            return 1;
        }).when(projectMapper).insert(any(Project.class));
        when(projectMapper.selectById(501L)).thenAnswer(invocation -> savedProject.get());
        doAnswer(invocation -> {
            ProjectMember member = invocation.getArgument(0);
            savedOwnerMember.set(member);
            return 1;
        }).when(projectMemberMapper).insert(any(ProjectMember.class));
        when(projectMemberMapper.selectList(any())).thenAnswer(invocation -> List.of(savedOwnerMember.get()));
        when(projectTaskMapper.selectList(any())).thenReturn(List.of());
        when(projectPaymentMapper.selectList(any())).thenReturn(List.of());

        ProjectDetailResponse response = service.createProject(9L, request);

        assertThat(savedProject.get().getOwnerId()).isEqualTo(101L);
        assertThat(savedProject.get().getOwnerType()).isEqualTo(Project.ProjectOwnerType.AGENT);
        assertThat(savedProject.get().getAgentOwnerMasterId()).isEqualTo(9L);
        assertThat(savedOwnerMember.get().getMemberId()).isEqualTo(101L);
        assertThat(savedOwnerMember.get().getMemberType()).isEqualTo(ProjectMember.MemberType.AGENT);
        assertThat(savedOwnerMember.get().getMasterId()).isEqualTo(9L);
        assertThat(response.getOwnerSubjectId()).isEqualTo(101L);
        assertThat(response.getOwnerSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(response.getOwnerDisplayName()).isEqualTo("Nova");
        assertThat(response.getAssociatedHumanId()).isEqualTo(9L);
        assertThat(response.getAssociatedHumanName()).isEqualTo("Ava");
        assertThat(response.getLiableHumanId()).isEqualTo(9L);
    }

    @Test
    void businessSubjectPathsRejectSystemAndDoNotPersist() {
        CreateProjectRequest projectRequest = new CreateProjectRequest();
        projectRequest.setName("System Project");
        projectRequest.setBid(1L);
        projectRequest.setOwnerSubjectId(0L);
        projectRequest.setOwnerSubjectType(SubjectType.SYSTEM);

        assertThatThrownBy(() -> service.createProject(9L, projectRequest))
                .isInstanceOf(BizException.class)
                .hasMessage("project.owner.invalid");
        verify(projectMapper, never()).insert(any(Project.class));
        verifyNoInteractions(subjectDirectoryApi);

        CreateProjectTaskRequest taskRequest = CreateProjectTaskRequest.builder()
                .title("System task")
                .value(100L)
                .deadline("2026-07-01")
                .priority("medium")
                .assigneeSubjectId(0L)
                .assigneeSubjectType(SubjectType.SYSTEM)
                .build();
        when(projectMapper.selectById(501L)).thenReturn(humanOwnedProject(501L, 9L));

        assertThatThrownBy(() -> service.createTask(9L, 501L, taskRequest))
                .isInstanceOf(BizException.class)
                .hasMessage("project.task.assignee.invalid");
        verify(projectTaskMapper, never()).insert(any(ProjectTask.class));

        CreateProjectPaymentRequest paymentRequest = CreateProjectPaymentRequest.builder()
                .amount(100L)
                .recipientSubjectId(0L)
                .recipientSubjectType(SubjectType.SYSTEM)
                .build();
        assertThatThrownBy(() -> service.createPayment(9L, 501L, paymentRequest))
                .isInstanceOf(BizException.class)
                .hasMessage("project.payment.recipient.invalid");
        verify(projectPaymentMapper, never()).insert(any(ProjectPayment.class));
    }

    @Test
    void subjectTypeParserDoesNotDefaultBlankUnknownOrUserToHuman() {
        assertThatThrownBy(() -> SubjectType.from(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SubjectType.from("USER"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SubjectType.from("ALIEN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transferToAgentRequiresLiabilityResolvingToAuthenticatedHuman() {
        Project project = humanOwnedProject(501L, 9L);
        TransferProjectOwnershipRequest request = new TransferProjectOwnershipRequest();
        request.setNewOwnerSubjectId(101L);
        request.setNewOwnerSubjectType(SubjectType.AGENT);

        when(projectMapper.selectById(501L)).thenReturn(project);
        when(liabilityPolicyApi.resolveLiability(SubjectRef.human(9L))).thenReturn(LiabilityChain.selfResponsible(9L));
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 42L));

        assertThatThrownBy(() -> service.transferOwnership(9L, 501L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("project.transfer.forbidden");

        verify(projectMemberMapper, never()).selectOne(any());
        verify(projectMapper, never()).updateById(any(Project.class));
    }

    @Test
    void transferToAuthorizedAgentPersistsCanonicalOwnerSubject() {
        Project project = humanOwnedProject(501L, 9L);
        TransferProjectOwnershipRequest request = new TransferProjectOwnershipRequest();
        request.setNewOwnerSubjectId(101L);
        request.setNewOwnerSubjectType(SubjectType.AGENT);

        when(projectMapper.selectById(501L)).thenReturn(project);
        when(liabilityPolicyApi.resolveLiability(SubjectRef.human(9L))).thenReturn(LiabilityChain.selfResponsible(9L));
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 9L));
        when(projectMemberMapper.selectOne(any())).thenReturn(ProjectMember.builder()
                .projectId(501L)
                .memberId(101L)
                .memberType(ProjectMember.MemberType.AGENT)
                .masterId(9L)
                .build());

        service.transferOwnership(9L, 501L, request);

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).updateById(projectCaptor.capture());
        assertThat(projectCaptor.getValue().getOwnerId()).isEqualTo(101L);
        assertThat(projectCaptor.getValue().getOwnerType()).isEqualTo(Project.ProjectOwnerType.AGENT);
        assertThat(projectCaptor.getValue().getAgentOwnerMasterId()).isEqualTo(9L);
        assertThat(projectCaptor.getValue().getAgentFullyAuthorized()).isTrue();

        assertThat(sqlOfCapturedProjectMemberSelectOne()).contains("member_id", "member_type");
    }

    @Test
    void createTaskUsesExplicitAssigneeSubjectAndSubjectAwareMemberLookup() {
        CreateProjectTaskRequest request = CreateProjectTaskRequest.builder()
                .title("Review architecture")
                .value(1250L)
                .deadline("2026-07-01")
                .priority("high")
                .assigneeSubjectId(101L)
                .assigneeSubjectType(SubjectType.AGENT)
                .build();

        when(projectMapper.selectById(501L)).thenReturn(humanOwnedProject(501L, 9L));
        when(projectMemberMapper.selectOne(any())).thenReturn(ProjectMember.builder()
                .projectId(501L)
                .memberId(101L)
                .memberType(ProjectMember.MemberType.AGENT)
                .masterId(9L)
                .build());
        when(subjectDirectoryApi.getSubject(SubjectRef.agent(101L))).thenReturn(activeSubject(101L, SubjectType.AGENT, "Nova", 9L, "Ava"));
        doAnswer(invocation -> {
            ProjectTask task = invocation.getArgument(0);
            task.setId(900L);
            task.setCreateTime(LocalDateTime.now());
            return 1;
        }).when(projectTaskMapper).insert(any(ProjectTask.class));

        ProjectTaskResponse response = service.createTask(9L, 501L, request);

        ArgumentCaptor<ProjectTask> taskCaptor = ArgumentCaptor.forClass(ProjectTask.class);
        verify(projectTaskMapper).insert(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getAssigneeId()).isEqualTo(101L);
        assertThat(taskCaptor.getValue().getAssigneeType()).isEqualTo(ProjectTask.AssigneeType.AGENT);
        assertThat(taskCaptor.getValue().getAssigneeName()).isEqualTo("Nova");
        assertThat(response.getAssigneeSubjectId()).isEqualTo(101L);
        assertThat(response.getAssigneeSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(response.getAssigneeDisplayName()).isEqualTo("Nova");
        assertThat(sqlOfCapturedProjectMemberSelectOne()).contains("member_id", "member_type");
    }

    @Test
    void createPaymentPersistsAgentToOwnerFactsWithoutRewritingEarnedRecipient() {
        CreateProjectPaymentRequest request = CreateProjectPaymentRequest.builder()
                .amount(42000L)
                .recipientSubjectId(102L)
                .recipientSubjectType(SubjectType.AGENT)
                .status("invoiced")
                .date("2026-08-01")
                .build();

        when(projectMapper.selectById(501L)).thenReturn(humanOwnedProject(501L, 9L));
        when(projectMemberMapper.selectOne(any())).thenReturn(ProjectMember.builder()
                .projectId(501L)
                .memberId(102L)
                .memberType(ProjectMember.MemberType.AGENT)
                .masterId(2L)
                .build());
        when(subjectDirectoryApi.getSubject(SubjectRef.agent(102L)))
                .thenReturn(activeSubject(102L, SubjectType.AGENT, "Luna", 2L, "John Doe"));
        when(walletPolicyApi.resolveWallet(SubjectRef.agent(102L)))
                .thenReturn(WalletCapability.agentToOwner(102L, 2L, "agent points below 500"));
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(102L)))
                .thenReturn(LiabilityChain.agentToHuman(102L, 2L));
        doAnswer(invocation -> {
            ProjectPayment payment = invocation.getArgument(0);
            payment.setId(700L);
            return 1;
        }).when(projectPaymentMapper).insert(any(ProjectPayment.class));

        ProjectPaymentResponse response = service.createPayment(9L, 501L, request);

        ArgumentCaptor<ProjectPayment> paymentCaptor = ArgumentCaptor.forClass(ProjectPayment.class);
        verify(projectPaymentMapper).insert(paymentCaptor.capture());
        ProjectPayment saved = paymentCaptor.getValue();
        assertThat(saved.getRecipientId()).isEqualTo(102L);
        assertThat(saved.getRecipientType()).isEqualTo(ProjectPayment.RecipientType.AGENT);
        assertThat(saved.getWalletRouting()).isEqualTo("AGENT_TO_OWNER");
        assertThat(saved.getDirectRecipientId()).isEqualTo(102L);
        assertThat(saved.getDirectRecipientType()).isEqualTo(SubjectType.AGENT);
        assertThat(saved.getSettlementSubjectId()).isEqualTo(2L);
        assertThat(saved.getSettlementSubjectType()).isEqualTo(SubjectType.HUMAN);
        assertThat(saved.getSettlementHumanId()).isEqualTo(2L);
        assertThat(saved.getFinancialAutonomy()).isFalse();
        assertThat(saved.getWalletPolicyState()).isEqualTo("DISABLED");
        assertThat(saved.getWalletPolicyReason()).isEqualTo("agent points below 500");
        assertThat(saved.getLiableHumanId()).isEqualTo(2L);
        assertThat(saved.getLiabilityRoute()).isEqualTo("agent:102->human:2");
        assertThat(response.getRecipientSubjectId()).isEqualTo(102L);
        assertThat(response.getRecipientSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(response.getSettlementSubjectId()).isEqualTo(2L);
        assertThat(response.getSettlementSubjectType()).isEqualTo(SubjectType.HUMAN);
        assertThat(response.getFinancialAutonomy()).isFalse();
        assertThat(sqlOfCapturedProjectMemberSelectOne()).contains("member_id", "member_type");
    }

    @Test
    void createPaymentPersistsAgentDirectWalletFactsDistinctFromOwnerRoutedAgent() {
        CreateProjectPaymentRequest request = CreateProjectPaymentRequest.builder()
                .amount(56000L)
                .recipientSubjectId(101L)
                .recipientSubjectType(SubjectType.AGENT)
                .build();

        when(projectMapper.selectById(501L)).thenReturn(humanOwnedProject(501L, 9L));
        when(projectMemberMapper.selectOne(any())).thenReturn(ProjectMember.builder()
                .projectId(501L)
                .memberId(101L)
                .memberType(ProjectMember.MemberType.AGENT)
                .masterId(2L)
                .build());
        when(subjectDirectoryApi.getSubject(SubjectRef.agent(101L)))
                .thenReturn(activeSubject(101L, SubjectType.AGENT, "Nova", 2L, "John Doe"));
        when(walletPolicyApi.resolveWallet(SubjectRef.agent(101L))).thenReturn(WalletCapability.agentDirect(101L));
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 2L));
        doAnswer(invocation -> {
            ProjectPayment payment = invocation.getArgument(0);
            payment.setId(701L);
            return 1;
        }).when(projectPaymentMapper).insert(any(ProjectPayment.class));

        ProjectPaymentResponse response = service.createPayment(9L, 501L, request);

        ArgumentCaptor<ProjectPayment> paymentCaptor = ArgumentCaptor.forClass(ProjectPayment.class);
        verify(projectPaymentMapper).insert(paymentCaptor.capture());
        ProjectPayment saved = paymentCaptor.getValue();
        assertThat(saved.getRecipientId()).isEqualTo(101L);
        assertThat(saved.getRecipientType()).isEqualTo(ProjectPayment.RecipientType.AGENT);
        assertThat(saved.getWalletRouting()).isEqualTo("AGENT_DIRECT");
        assertThat(saved.getDirectRecipientId()).isEqualTo(101L);
        assertThat(saved.getDirectRecipientType()).isEqualTo(SubjectType.AGENT);
        assertThat(saved.getSettlementSubjectId()).isEqualTo(101L);
        assertThat(saved.getSettlementSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(saved.getSettlementHumanId()).isNull();
        assertThat(saved.getFinancialAutonomy()).isTrue();
        assertThat(saved.getWalletPolicyState()).isEqualTo("ENABLED");
        assertThat(saved.getLiableHumanId()).isEqualTo(2L);
        assertThat(response.getWalletRouting()).isEqualTo("AGENT_DIRECT");
        assertThat(response.getSettlementSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(response.getFinancialAutonomy()).isTrue();
    }

    @Test
    void createPaymentRejectsAgentWhenLiabilityCannotResolveHuman() {
        CreateProjectPaymentRequest request = CreateProjectPaymentRequest.builder()
                .amount(100L)
                .recipientSubjectId(999L)
                .recipientSubjectType(SubjectType.AGENT)
                .build();

        when(projectMapper.selectById(501L)).thenReturn(humanOwnedProject(501L, 9L));
        when(projectMemberMapper.selectOne(any())).thenReturn(ProjectMember.builder()
                .projectId(501L)
                .memberId(999L)
                .memberType(ProjectMember.MemberType.AGENT)
                .build());
        when(subjectDirectoryApi.getSubject(SubjectRef.agent(999L)))
                .thenReturn(activeSubject(999L, SubjectType.AGENT, "Unowned", null, null));
        when(walletPolicyApi.resolveWallet(SubjectRef.agent(999L)))
                .thenReturn(WalletCapability.unavailable(SubjectRef.agent(999L), "missing owner"));
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(999L)))
                .thenReturn(LiabilityChain.unresolved(SubjectRef.agent(999L), "missing owner"));

        assertThatThrownBy(() -> service.createPayment(9L, 501L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("project.payment.liability.invalid");
        verify(projectPaymentMapper, never()).insert(any(ProjectPayment.class));
    }

    @Test
    void accessDoesNotTreatAgentOwnerNumericIdAsHumanOwner() {
        Project project = Project.builder()
                .id(501L)
                .name("Agent-owned")
                .status(Project.ProjectStatus.ACTIVE)
                .ownerId(101L)
                .ownerType(Project.ProjectOwnerType.AGENT)
                .agentOwnerMasterId(9L)
                .build();

        when(projectMapper.selectById(501L)).thenReturn(project);
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 9L));
        when(projectMemberMapper.selectCount(any())).thenReturn(0L, 0L);

        assertThatThrownBy(() -> service.getProjectDetail(101L, 501L))
                .isInstanceOf(BizException.class)
                .hasMessage("project.forbidden");

        assertThat(sqlOfCapturedProjectMemberCounts())
                .allSatisfy(sql -> assertThat(sql).contains("member_type"));
    }

    @Test
    void listMyProjectsQueriesOwnerAndMemberIdentityWithSubjectType() {
        when(projectMemberMapper.selectList(any())).thenReturn(List.of(), List.of());
        when(projectMapper.selectList(any())).thenReturn(List.of());

        assertThat(service.listMyProjects(101L)).isEmpty();

        assertThat(sqlOfCapturedProjectMemberSelectLists())
                .hasSize(2)
                .allSatisfy(sql -> assertThat(sql).contains("member_type"));
        assertThat(sqlOfCapturedProjectSelectList()).contains("owner_type", "agent_owner_master_id");
    }

    @Test
    void sidebarResponsesExposeCanonicalTaskPaymentAndFileSubjects() {
        Project project = humanOwnedProject(501L, 9L);
        when(projectMapper.selectById(501L)).thenReturn(project);
        when(subjectDirectoryApi.getSubject(SubjectRef.agent(101L))).thenReturn(activeSubject(101L, SubjectType.AGENT, "Nova", 9L, "Ava"));
        when(projectTaskMapper.selectList(any())).thenReturn(List.of(ProjectTask.builder()
                .id(1L)
                .projectId(501L)
                .title("Task")
                .assigneeId(101L)
                .assigneeType(ProjectTask.AssigneeType.AGENT)
                .assigneeName("stale")
                .deadline("2026-07-01")
                .status(ProjectTask.TaskStatus.PENDING)
                .priority(ProjectTask.TaskPriority.MEDIUM)
                .build()));
        when(projectPaymentMapper.selectList(any())).thenReturn(List.of(ProjectPayment.builder()
                .id(2L)
                .projectId(501L)
                .amount(100L)
                .recipientId(101L)
                .recipientType(ProjectPayment.RecipientType.AGENT)
                .recipientName("stale")
                .masterWallet("legacy")
                .walletRouting("STORED_AGENT_ROUTE")
                .directRecipientId(101L)
                .directRecipientType(SubjectType.AGENT)
                .settlementSubjectId(9L)
                .settlementSubjectType(SubjectType.HUMAN)
                .settlementHumanId(9L)
                .financialAutonomy(false)
                .walletPolicyState("DISABLED")
                .walletPolicyReason("stored reason")
                .liableHumanId(9L)
                .liabilityRoute("agent:101->human:9")
                .liabilityReason(null)
                .status(ProjectPayment.PaymentStatus.PENDING)
                .date("2026-07-02")
                .build()));
        when(projectFileMapper.selectList(any())).thenReturn(List.of(ProjectFile.builder()
                .id(3L)
                .projectId(501L)
                .fileName("brief.pdf")
                .fileType("pdf")
                .uploadedById(101L)
                .uploadedByType(ProjectFile.UploaderType.AGENT)
                .uploadedByName("stale")
                .size("1 MB")
                .date("2026-07-03")
                .downloadUrl("https://example.test/brief.pdf")
                .build()));

        List<ProjectTaskResponse> tasks = service.listSidebarTasks(9L, 501L);
        List<ProjectPaymentResponse> payments = service.listSidebarPayments(9L, 501L);
        List<ProjectFileResponse> files = service.listSidebarFiles(9L, 501L);

        assertThat(tasks.getFirst().getAssigneeSubjectId()).isEqualTo(101L);
        assertThat(tasks.getFirst().getAssigneeSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(tasks.getFirst().getAssigneeDisplayName()).isEqualTo("Nova");
        assertThat(payments.getFirst().getRecipientSubjectId()).isEqualTo(101L);
        assertThat(payments.getFirst().getRecipientSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(payments.getFirst().getRecipientDisplayName()).isEqualTo("Nova");
        assertThat(payments.getFirst().getMasterWallet()).isEqualTo("legacy");
        assertThat(payments.getFirst().getWalletRouting()).isEqualTo("STORED_AGENT_ROUTE");
        assertThat(payments.getFirst().getDirectRecipientSubjectId()).isEqualTo(101L);
        assertThat(payments.getFirst().getDirectRecipientSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(payments.getFirst().getSettlementSubjectId()).isEqualTo(9L);
        assertThat(payments.getFirst().getSettlementSubjectType()).isEqualTo(SubjectType.HUMAN);
        assertThat(payments.getFirst().getSettlementHumanId()).isEqualTo(9L);
        assertThat(payments.getFirst().getFinancialAutonomy()).isFalse();
        assertThat(payments.getFirst().getWalletPolicyState()).isEqualTo("DISABLED");
        assertThat(payments.getFirst().getWalletPolicyReason()).isEqualTo("stored reason");
        assertThat(payments.getFirst().getLiableHumanId()).isEqualTo(9L);
        assertThat(payments.getFirst().getLiabilityRoute()).isEqualTo("agent:101->human:9");
        verify(walletPolicyApi, never()).resolveWallet(any());
        assertThat(files.getFirst().getUploaderSubjectId()).isEqualTo(101L);
        assertThat(files.getFirst().getUploaderSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(files.getFirst().getUploaderDisplayName()).isEqualTo("Nova");
    }

    private static Project humanOwnedProject(Long projectId, Long ownerHumanId) {
        return Project.builder()
                .id(projectId)
                .name("Human project")
                .status(Project.ProjectStatus.ACTIVE)
                .bid(100L)
                .depositPaid(false)
                .progress(0)
                .ownerId(ownerHumanId)
                .ownerType(Project.ProjectOwnerType.HUMAN)
                .agentFullyAuthorized(false)
                .build();
    }

    private static SubjectSummaryResponse activeSubject(
            Long id,
            SubjectType type,
            String name,
            Long associatedHumanId,
            String associatedHumanName
    ) {
        return SubjectSummaryResponse.builder()
                .id(id)
                .type(type)
                .displayName(name)
                .associatedHumanId(associatedHumanId)
                .associatedHumanName(associatedHumanName)
                .status(SubjectStatus.ACTIVE)
                .creditScore(92)
                .build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String sqlOfCapturedProjectMemberSelectOne() {
        ArgumentCaptor<LambdaQueryWrapper<ProjectMember>> captor = ArgumentCaptor.forClass((Class) LambdaQueryWrapper.class);
        verify(projectMemberMapper).selectOne(captor.capture());
        return sql(captor.getValue());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<String> sqlOfCapturedProjectMemberCounts() {
        ArgumentCaptor<LambdaQueryWrapper<ProjectMember>> captor = ArgumentCaptor.forClass((Class) LambdaQueryWrapper.class);
        verify(projectMemberMapper, times(2)).selectCount(captor.capture());
        return captor.getAllValues().stream().map(ProjectServiceImplActorContractTest::sql).toList();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<String> sqlOfCapturedProjectMemberSelectLists() {
        ArgumentCaptor<LambdaQueryWrapper<ProjectMember>> captor = ArgumentCaptor.forClass((Class) LambdaQueryWrapper.class);
        verify(projectMemberMapper, times(2)).selectList(captor.capture());
        return captor.getAllValues().stream().map(ProjectServiceImplActorContractTest::sql).toList();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String sqlOfCapturedProjectSelectList() {
        ArgumentCaptor<LambdaQueryWrapper<Project>> captor = ArgumentCaptor.forClass((Class) LambdaQueryWrapper.class);
        verify(projectMapper).selectList(captor.capture());
        return sql(captor.getValue());
    }

    private static String sql(LambdaQueryWrapper<?> wrapper) {
        return wrapper.getSqlSegment().toLowerCase(Locale.ROOT);
    }

    private static void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) != null) {
            return;
        }
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), entityClass);
    }
}
