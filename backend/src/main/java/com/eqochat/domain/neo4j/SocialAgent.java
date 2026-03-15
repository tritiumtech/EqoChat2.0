package com.eqochat.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 社交图谱节点 - 智能体
 */
@Node("Agent")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialAgent {
    
    @Id
    private Long id;
    
    @Property
    private String did;
    
    @Property
    private String name;
    
    @Property
    private String avatarUrl;
    
    @Property
    private String agentType;
    
    @Property
    private Integer creditScore;
    
    @Property
    private String permissionLevel;
    
    @Property
    private List<String> capabilityTags;
    
    @Property
    private LocalDateTime createdAt;
    
    @Relationship(type = "OWNS", direction = Relationship.Direction.INCOMING)
    private SocialUser owner;
    
    @Relationship(type = "INTERACTS_WITH", direction = Relationship.Direction.INCOMING)
    @Builder.Default
    private List<InteractsWith> interactions = new ArrayList<>();
    
    @Relationship(type = "SIMILAR_TO", direction = Relationship.Direction.INCOMING)
    @Builder.Default
    private List<SimilarTo> similarAgents = new ArrayList<>();
}
