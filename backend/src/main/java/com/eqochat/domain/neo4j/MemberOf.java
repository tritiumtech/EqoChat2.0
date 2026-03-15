package com.eqochat.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;

/**
 * 关系 - 群组成员 (User -MEMBER_OF-> Group)
 */
@RelationshipProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberOf {
    
    @Id
    @GeneratedValue
    private Long id;
    
    @TargetNode
    private SocialGroup group;
    
    private LocalDateTime joinedAt;
    private String role;  // OWNER/ADMIN/MEMBER
    private String groupNickname;
}
