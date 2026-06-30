package com.eqochat.business.actor.api.dto.response;

import com.eqochat.business.actor.api.model.CapabilitySet;
import com.eqochat.business.actor.api.model.CreditProfileSummary;
import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectStatus;
import com.eqochat.business.actor.api.model.SubjectType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectSummaryResponse {

    private Long id;
    private SubjectType type;
    private String did;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private SubjectStatus status;
    private Integer points;
    private Integer creditScore;
    private CreditProfileSummary creditProfile;
    private LiabilityChain liabilityChain;
    private CapabilitySet capabilities;
    private List<String> capabilityTags;
    private Long associatedHumanId;
    private String associatedHumanName;
}
