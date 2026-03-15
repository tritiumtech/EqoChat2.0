package com.eqochat.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Properties;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;
import java.util.Map;

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
    
    @Properties
    private Map<String, Object> properties;
    
    private LocalDateTime joinedAt;
    private String role;  // OWNER/ADMIN/MEMBER
    private String groupNickname;
}
