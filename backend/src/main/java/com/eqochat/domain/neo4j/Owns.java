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
 * 关系 - 拥有 (User -OWNS-> Agent)
 */
@RelationshipProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Owns {
    
    @Id
    @GeneratedValue
    private Long id;
    
    @TargetNode
    private SocialAgent agent;
    
    private LocalDateTime createdAt;
    private String bindingType;  // OWNER/OPERATOR/VIEWER
    private Boolean liabilityAccepted;
}
