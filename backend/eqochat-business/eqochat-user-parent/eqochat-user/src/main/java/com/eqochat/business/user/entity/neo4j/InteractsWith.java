package com.eqochat.business.user.entity.neo4j;

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
 * 关系 - 交互 (User/Agent -INTERACTS_WITH-> User/Agent)
 * 记录社交互动强度
 */
@RelationshipProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractsWith {
    
    @Id
    @GeneratedValue
    private Long id;
    
    @TargetNode
    private Object target;  // SocialUser or SocialAgent
    
    private Integer interactionCount;  // 交互次数
    private Integer interactionScore;  // 交互强度分数
    private LocalDateTime lastInteractionAt;
    private String[] interactionTypes;  // MESSAGE/CALL/SHARE/etc
}
