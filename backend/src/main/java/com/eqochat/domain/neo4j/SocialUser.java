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
 * 社交图谱节点 - 用户
 */
@Node("User")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialUser {
    
    @Id
    private Long id;
    
    @Property
    private String did;
    
    @Property
    private String nickname;
    
    @Property
    private String avatarUrl;
    
    @Property
    private Integer creditScore;
    
    @Property
    private LocalDateTime createdAt;
    
    @Relationship(type = "FOLLOWS", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private List<Follows> following = new ArrayList<>();
    
    @Relationship(type = "FOLLOWS", direction = Relationship.Direction.INCOMING)
    @Builder.Default
    private List<Follows> followers = new ArrayList<>();
    
    @Relationship(type = "FRIEND_WITH", direction = Relationship.Direction.UNDIRECTED)
    @Builder.Default
    private List<FriendWith> friends = new ArrayList<>();
    
    @Relationship(type = "OWNS", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private List<Owns> agents = new ArrayList<>();
    
    @Relationship(type = "MEMBER_OF", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private List<MemberOf> groups = new ArrayList<>();
}
