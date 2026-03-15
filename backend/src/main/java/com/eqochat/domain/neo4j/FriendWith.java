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
 * 关系 - 好友 (User -FRIEND_WITH- User)
 */
@RelationshipProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendWith {
    
    @Id
    @GeneratedValue
    private Long id;
    
    @TargetNode
    private SocialUser friend;
    
    @Properties
    private Map<String, Object> properties;
    
    private LocalDateTime createdAt;
    private String status;  // ACTIVE/DELETED/BLOCKED
    private String addSource;
}
