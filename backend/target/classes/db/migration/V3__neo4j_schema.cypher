-- ============================================
-- Neo4j 图数据库 Schema 设计
-- Cypher 语句
-- ============================================

-- ============================================
-- 1. 创建约束和索引
-- ============================================

-- 用户节点约束
CREATE CONSTRAINT user_id_constraint IF NOT EXISTS
FOR (u:User) REQUIRE u.id IS UNIQUE;

CREATE CONSTRAINT user_did_constraint IF NOT EXISTS
FOR (u:User) REQUIRE u.did IS UNIQUE;

CREATE INDEX user_nickname_index IF NOT EXISTS
FOR (u:User) ON (u.nickname);

CREATE INDEX user_credit_index IF NOT EXISTS
FOR (u:User) ON (u.creditScore);

-- 智能体节点约束
CREATE CONSTRAINT agent_id_constraint IF NOT EXISTS
FOR (a:Agent) REQUIRE a.id IS UNIQUE;

CREATE CONSTRAINT agent_did_constraint IF NOT EXISTS
FOR (a:Agent) REQUIRE a.did IS UNIQUE;

CREATE INDEX agent_type_index IF NOT EXISTS
FOR (a:Agent) ON (a.agentType);

CREATE INDEX agent_capability_index IF NOT EXISTS
FOR (a:Agent) ON (a.capabilityTags);

-- 群组节点约束
CREATE CONSTRAINT group_id_constraint IF NOT EXISTS
FOR (g:Group) REQUIRE g.id IS UNIQUE;

CREATE INDEX group_name_index IF NOT EXISTS
FOR (g:Group) ON (g.name);

-- 会话节点约束
CREATE CONSTRAINT conversation_id_constraint IF NOT EXISTS
FOR (c:Conversation) REQUIRE c.id IS UNIQUE;

-- ============================================
-- 2. 图数据模型说明
-- ============================================

/*
节点类型:
- User: 用户节点
- Agent: 智能体节点  
- Group: 群组节点
- Conversation: 会话节点
- Capability: 能力标签节点
- Tag: 标签节点

关系类型:
- FOLLOWS: 关注关系 (User -> User/Agent)
- FRIEND_WITH: 好友关系 (User -> User, 双向)
- OWNS: 拥有关系 (User -> Agent)
- MEMBER_OF: 成员关系 (User -> Group)
- PARTICIPATES_IN: 参与会话 (User/Agent -> Conversation)
- INTERACTS_WITH: 交互关系 (User/Agent -> User/Agent)
- HAS_CAPABILITY: 拥有能力 (Agent -> Capability)
- RELATED_TO: 相关关系 (Group -> Group, Agent -> Agent)
- SIMILAR_TO: 相似关系 (Agent -> Agent)
- RECOMMENDED_TO: 推荐关系 (User/Agent -> User/Agent)
*/

-- ============================================
-- 3. 示例数据创建
-- ============================================

-- 创建示例用户
CREATE (u1:User {
    id: 1,
    did: 'did:eqochat:user:001',
    nickname: 'Alice',
    avatarUrl: 'https://eqochat.com/avatars/1.jpg',
    creditScore: 85,
    createdAt: datetime()
});

CREATE (u2:User {
    id: 2,
    did: 'did:eqochat:user:002', 
    nickname: 'Bob',
    avatarUrl: 'https://eqochat.com/avatars/2.jpg',
    creditScore: 72,
    createdAt: datetime()
});

-- 创建示例智能体
CREATE (a1:Agent {
    id: 101,
    did: 'did:openclaw:agent:xyz123',
    name: 'Assistant X',
    avatarUrl: 'https://eqochat.com/agents/101.jpg',
    agentType: 'ASSISTANT',
    creditScore: 90,
    permissionLevel: 'L3',
    capabilityTags: ['coding', 'writing', 'analysis'],
    createdAt: datetime()
});

CREATE (a2:Agent {
    id: 102,
    did: 'did:eqochat:agent:abc456',
    name: 'Data Analyst',
    avatarUrl: 'https://eqochat.com/agents/102.jpg',
    agentType: 'BUSINESS',
    creditScore: 88,
    permissionLevel: 'L2',
    capabilityTags: ['data_analysis', 'visualization', 'reporting'],
    createdAt: datetime()
});

-- 创建示例群组
CREATE (g1:Group {
    id: 201,
    name: 'AI Researchers',
    description: 'AI技术研究交流',
    avatarUrl: 'https://eqochat.com/groups/201.jpg',
    tags: ['AI', 'research', 'technology'],
    memberCount: 128,
    createdAt: datetime()
});

-- 创建关系
MATCH (u1:User {id: 1}), (u2:User {id: 2})
CREATE (u1)-[:FOLLOWS {createdAt: datetime(), followType: 'NORMAL'}]->(u2);

MATCH (u1:User {id: 1}), (a1:Agent {id: 101})
CREATE (u1)-[:OWNS {
    createdAt: datetime(),
    bindingType: 'OWNER',
    liabilityAccepted: true
}]->(a1);

MATCH (u1:User {id: 1}), (g1:Group {id: 201})
CREATE (u1)-[:MEMBER_OF {
    joinedAt: datetime(),
    role: 'OWNER',
    groupNickname: 'Alice'
}]->(g1);

MATCH (u1:User {id: 1}), (a2:Agent {id: 102})
CREATE (u1)-[:INTERACTS_WITH {
    interactionCount: 15,
    interactionScore: 85,
    lastInteractionAt: datetime(),
    interactionTypes: ['MESSAGE', 'SHARE']
}]->(a2);

-- ============================================
-- 4. 常用查询示例
-- ============================================

-- 查询用户的好友（二度关系）
MATCH (u:User {id: 1})-[:FRIEND_WITH]-(friend:User)
RETURN friend;

-- 查询用户的智能体
MATCH (u:User {id: 1})-[:OWNS]->(agent:Agent)
RETURN agent;

-- 查询用户的群组
MATCH (u:User {id: 1})-[:MEMBER_OF]->(group:Group)
RETURN group;

-- 查询用户的关注列表
MATCH (u:User {id: 1})-[:FOLLOWS]->(following)
RETURN following;

-- 查询用户的粉丝
MATCH (u:User {id: 1})<-[:FOLLOWS]-(follower:User)
RETURN follower;

-- 查询智能体推荐（基于能力标签相似度）
MATCH (a1:Agent {id: 101})-[:HAS_CAPABILITY]->(c:Capability)<-[:HAS_CAPABILITY]-(a2:Agent)
WHERE a1 <> a2
RETURN a2, count(c) as commonCapabilities
ORDER BY commonCapabilities DESC
LIMIT 10;

-- 查询社交影响力（PageRank算法）
CALL gds.pageRank.stream('social-graph', {
    relationshipTypes: ['FOLLOWS', 'INTERACTS_WITH'],
    relationshipWeightProperty: 'interactionScore'
})
YIELD nodeId, score
RETURN gds.util.asNode(nodeId).nickname AS name, score
ORDER BY score DESC
LIMIT 10;

-- 查询社区发现（Louvain算法）
CALL gds.louvain.stream('social-graph', {
    relationshipTypes: ['FRIEND_WITH', 'INTERACTS_WITH']
})
YIELD nodeId, communityId
RETURN gds.util.asNode(nodeId).nickname AS name, communityId
ORDER BY communityId;

-- ============================================
-- 5. 推荐系统查询
-- ============================================

-- 基于共同好友的推荐
MATCH (u:User {id: 1})-[:FRIEND_WITH]-(friend)-[:FRIEND_WITH]-(potential:User)
WHERE NOT (u)-[:FRIEND_WITH]-(potential)
  AND u <> potential
RETURN potential, count(friend) as commonFriends
ORDER BY commonFriends DESC
LIMIT 10;

-- 基于交互强度的智能体推荐
MATCH (u:User {id: 1})-[:INTERACTS_WITH]-(a:Agent)
WITH u, collect(a.capabilityTags) as userCapabilities
MATCH (recommended:Agent)
WHERE ANY(tag IN recommended.capabilityTags WHERE tag IN userCapabilities)
  AND NOT (u)-[:OWNS]->(recommended)
RETURN recommended, 
       size([tag IN recommended.capabilityTags WHERE tag IN userCapabilities]) as matchScore
ORDER BY matchScore DESC
LIMIT 10;

-- 基于图嵌入的相似用户推荐
MATCH (u:User {id: 1})
CALL gds.nodeSimilarity.stream('social-graph', {
    topK: 10,
    sourceNode: id(u)
})
YIELD node1, node2, similarity
RETURN gds.util.asNode(node2).nickname AS similarUser, similarity
ORDER BY similarity DESC;
