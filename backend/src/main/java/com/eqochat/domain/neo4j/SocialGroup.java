package com.eqochat.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * 社交图谱节点 - 群组
 */
@Node("Group")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialGroup {
    
    @Id
    private Long id;
    
    @Property
    private String name;
    
    @Property
    private String description;
    
    @Property
    private String avatarUrl;
    
    @Property
    private List<String> tags;
    
    @Property
    private Integer memberCount;
    
    @Relationship(type = "MEMBER_OF", direction = Relationship.Direction.INCOMING)
    @Builder.Default
    private List<SocialUser> members = new ArrayList<>();
    
    @Relationship(type = "RELATED_TO", direction = Relationship.Direction.UNDIRECTED)
    @Builder.Default
    private List<RelatedTo> relatedGroups = new ArrayList<>();
}
