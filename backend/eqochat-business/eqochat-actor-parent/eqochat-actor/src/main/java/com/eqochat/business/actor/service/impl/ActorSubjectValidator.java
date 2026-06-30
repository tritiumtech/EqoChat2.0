package com.eqochat.business.actor.service.impl;

import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.agent.entity.AgentBinding;
import com.eqochat.business.agent.entity.AgentProfile;
import com.eqochat.business.agent.mapper.AgentBindingMapper;
import com.eqochat.business.agent.mapper.AgentProfileMapper;
import com.eqochat.business.user.entity.UserProfile;
import com.eqochat.business.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ActorSubjectValidator {

    private final UserProfileMapper userProfileMapper;
    private final AgentProfileMapper agentProfileMapper;
    private final AgentBindingMapper agentBindingMapper;

    ActorSubjectValidation validateOrdinaryCapabilities(SubjectRef ref) {
        ActorSubjectValidation base = validateActiveSubject(ref);
        if (!base.valid() || ref.type() != SubjectType.AGENT) {
            return base;
        }
        return validateAgentLiability(ref.id());
    }

    ActorSubjectValidation validateActiveSubject(SubjectRef ref) {
        if (ref == null) {
            return ActorSubjectValidation.invalid(null, "subject is null");
        }
        if (ref.id() == null) {
            return ActorSubjectValidation.invalid(ref, "subject id is null");
        }
        if (ref.type() == SubjectType.SYSTEM) {
            return ActorSubjectValidation.invalid(ref, "system subject has no ordinary actor capabilities");
        }
        if (ref.type() == SubjectType.HUMAN) {
            return validateHuman(ref.id());
        }
        if (ref.type() == SubjectType.AGENT) {
            AgentProfile agent = agentProfileMapper.selectById(ref.id());
            if (agent == null) {
                return ActorSubjectValidation.invalid(ref, "agent not found");
            }
            if (agent.getStatus() != AgentProfile.AgentStatus.ACTIVE) {
                return ActorSubjectValidation.invalid(ref, null, agent, null, "agent is not active");
            }
            return ActorSubjectValidation.valid(ref, null, agent, null, null);
        }
        return ActorSubjectValidation.invalid(ref, "unsupported subject type");
    }

    ActorSubjectValidation validateHuman(Long humanId) {
        SubjectRef ref = humanId == null ? null : SubjectRef.human(humanId);
        if (humanId == null) {
            return ActorSubjectValidation.invalid(ref, "human id is null");
        }
        UserProfile human = userProfileMapper.selectById(humanId);
        if (human == null) {
            return ActorSubjectValidation.invalid(ref, "human not found");
        }
        if (human.getStatus() != UserProfile.UserStatus.ACTIVE) {
            return ActorSubjectValidation.invalid(ref, human, null, null, "human is not active");
        }
        return ActorSubjectValidation.valid(ref, human, null, null, null);
    }

    ActorSubjectValidation validateAgentLiability(Long agentId) {
        SubjectRef ref = agentId == null ? null : SubjectRef.agent(agentId);
        if (agentId == null) {
            return ActorSubjectValidation.invalid(ref, "agent id is null");
        }

        AgentProfile agent = agentProfileMapper.selectById(agentId);
        if (agent == null) {
            return ActorSubjectValidation.invalid(ref, "agent not found");
        }
        if (agent.getStatus() != AgentProfile.AgentStatus.ACTIVE) {
            return ActorSubjectValidation.invalid(ref, null, agent, null, "agent is not active");
        }
        if (agent.getOwnerId() == null) {
            return ActorSubjectValidation.invalid(ref, null, agent, null, "agent owner is missing");
        }

        UserProfile owner = userProfileMapper.selectById(agent.getOwnerId());
        if (owner == null) {
            return ActorSubjectValidation.invalid(ref, null, agent, null, "owner human not found");
        }
        if (owner.getStatus() != UserProfile.UserStatus.ACTIVE) {
            return ActorSubjectValidation.invalid(ref, owner, agent, null, "owner human is not active");
        }

        AgentBinding binding = agentBindingMapper.findByAgentIdAndOwnerId(agent.getId(), agent.getOwnerId())
                .orElse(null);
        if (binding == null) {
            return ActorSubjectValidation.invalid(ref, owner, agent, null, "owner binding not found");
        }
        if (binding.getBindingType() != AgentBinding.BindingType.OWNER) {
            return ActorSubjectValidation.invalid(ref, owner, agent, binding, "owner binding type is not OWNER");
        }
        if (binding.getBindingStatus() != AgentBinding.BindingStatus.ACTIVE) {
            return ActorSubjectValidation.invalid(ref, owner, agent, binding, "owner binding is not active");
        }
        if (!Boolean.TRUE.equals(binding.getLiabilityAccepted())) {
            return ActorSubjectValidation.invalid(ref, owner, agent, binding, "owner liability is not accepted");
        }

        return ActorSubjectValidation.valid(ref, owner, agent, binding, null);
    }
}

record ActorSubjectValidation(
        SubjectRef ref,
        UserProfile human,
        AgentProfile agent,
        AgentBinding binding,
        String reason
) {

    static ActorSubjectValidation valid(
            SubjectRef ref,
            UserProfile human,
            AgentProfile agent,
            AgentBinding binding,
            String reason
    ) {
        return new ActorSubjectValidation(ref, human, agent, binding, reason);
    }

    static ActorSubjectValidation invalid(SubjectRef ref, String reason) {
        return new ActorSubjectValidation(ref, null, null, null, reason);
    }

    static ActorSubjectValidation invalid(
            SubjectRef ref,
            UserProfile human,
            AgentProfile agent,
            AgentBinding binding,
            String reason
    ) {
        return new ActorSubjectValidation(ref, human, agent, binding, reason);
    }

    boolean valid() {
        return reason == null;
    }
}
