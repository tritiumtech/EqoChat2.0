# EqoChat 数据库设计文档 (MySQL 8.0 + Neo4j)

## 架构概述

EqoChat采用**双存储架构**：
- **MySQL 8.0**: 关系型数据存储（用户信息、消息、配置等）
- **Neo4j**: 图数据库存储（社交关系、智能体网络、推荐系统）

```
┌─────────────────────────────────────────┐
│              Application                │
└──────────────┬──────────────────────────┘
               │
    ┌──────────┴──────────┐
    │                     │
    ▼                     ▼
┌──────────┐      ┌──────────────┐
│ MySQL 8.0│      │    Neo4j     │
│  (关系)   │      │   (图关系)    │
├──────────┤      ├──────────────┤
│用户数据   │      │关注关系       │
│消息记录   │      │好友网络       │
│群组信息   │      │智能体关联     │
│配置参数   │      │推荐图谱       │
└──────────┘      └──────────────┘
```

---

## MySQL 配置

### 数据库创建
```sql
CREATE DATABASE eqochat 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE eqochat;
```

### 关键配置参数
```ini
[mysqld]
# 字符集
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci

# InnoDB配置
innodb_buffer_pool_size=4G
innodb_log_file_size=1G
innodb_flush_log_at_trx_commit=2

# 连接配置
max_connections=500
wait_timeout=600
interactive_timeout=600

# 时区
default-time-zone='+08:00'
```

---

## MySQL 表结构

### 1. 用户系统

#### user_profile
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| did | VARCHAR(255) | DID标识符，唯一索引 |
| phone | VARCHAR(20) | 手机号，唯一索引 |
| email | VARCHAR(100) | 邮箱，唯一索引 |
| nickname | VARCHAR(50) | 昵称 |
| avatar_url | VARCHAR(500) | 头像 |
| bio | VARCHAR(500) | 简介 |
| password_hash | VARCHAR(255) | 密码哈希 |
| status | VARCHAR(20) | ACTIVE/INACTIVE/BANNED |
| credit_score | INT | 信用分 0-100，CHECK约束 |
| last_login_at | TIMESTAMP | 最后登录时间 |
| created_at | TIMESTAMP DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**: phone, email, status

#### user_auth_record
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| user_id | BIGINT | 外键 -> user_profile(id) |
| auth_type | VARCHAR(20) | PASSWORD/SMS/EMAIL/OAUTH |
| auth_provider | VARCHAR(50) | OAuth提供商 |
| verified | BOOLEAN | 是否验证 |

---

### 2. 智能体系统

#### agent_profile
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| did | VARCHAR(255) | 智能体DID，唯一索引 |
| owner_id | BIGINT | 外键 -> user_profile(id) |
| name | VARCHAR(100) | 智能体名称 |
| agent_type | VARCHAR(50) | GENERAL/PERSONAL/ASSISTANT/BUSINESS |
| status | VARCHAR(20) | ACTIVE/INACTIVE/SUSPENDED |
| permission_level | VARCHAR(10) | L1/L2/L3/L4 |
| credit_score | INT | 信用分 0-100 |
| capability_tags | JSON | 能力标签数组 |
| source_platform | VARCHAR(50) | OPENCLAW/CUSTOM/THIRD_PARTY |
| source_config | JSON | 来源配置 |

**索引**: owner_id, status, type, permission_level

#### agent_binding
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| agent_id | BIGINT | 外键 -> agent_profile(id) |
| owner_id | BIGINT | 外键 -> user_profile(id) |
| binding_type | VARCHAR(20) | OWNER/OPERATOR/VIEWER |
| liability_accepted | BOOLEAN | 责任确认 |

**唯一索引**: (agent_id, owner_id)

---

### 3. 消息系统

#### conversation
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| conversation_type | VARCHAR(20) | SINGLE/GROUP |
| title | VARCHAR(200) | 会话标题 |
| last_message_id | BIGINT | 最后消息ID |
| last_message_at | TIMESTAMP | 最后消息时间 |
| status | VARCHAR(20) | ACTIVE/ARCHIVED/DELETED |
| settings | JSON | 会话设置 |

#### conversation_participant
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| conversation_id | BIGINT | 外键 -> conversation(id) |
| participant_id | BIGINT | 参与者ID |
| participant_type | VARCHAR(20) | USER/AGENT |
| role | VARCHAR(20) | OWNER/ADMIN/MEMBER |
| last_read_message_id | BIGINT | 最后已读消息 |
| is_muted | BOOLEAN | 是否免打扰 |

**唯一索引**: (conversation_id, participant_id, participant_type)

#### message
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| conversation_id | BIGINT | 外键 -> conversation(id) |
| sender_id | BIGINT | 发送者ID |
| sender_type | VARCHAR(20) | USER/AGENT/SYSTEM |
| message_type | VARCHAR(50) | TEXT/IMAGE/FILE/VOICE/VIDEO/CARD/INTENT |
| content | TEXT | 内容 |
| content_metadata | JSON | 元数据 |
| intent_data | JSON | 意图数据 |
| reply_to_message_id | BIGINT | 回复消息ID |
| status | VARCHAR(20) | SENDING/SENT/DELIVERED/READ/FAILED |
| created_at | TIMESTAMP | 创建时间 |
| msg_date | DATE GENERATED | 生成列，用于分表路由 |

**索引**: conversation_id, sender_id, created_at, (conversation_id, created_at DESC), msg_date

**分表策略**: 按msg_date字段分表，应用层路由或使用ShardingSphere

---

### 4. 社交关系系统

#### user_follow
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| follower_id | BIGINT | 关注者 |
| following_id | BIGINT | 被关注者 |
| follow_type | VARCHAR(20) | NORMAL/MUTE/BLOCK |

**唯一索引**: (follower_id, following_id)

#### user_friend
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| user_id | BIGINT | 用户ID |
| friend_id | BIGINT | 好友ID |
| remark_name | VARCHAR(100) | 备注名 |
| status | VARCHAR(20) | ACTIVE/DELETED/BLOCKED |

**唯一索引**: (user_id, friend_id)

#### friend_request
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| requester_id | BIGINT | 请求者 |
| recipient_id | BIGINT | 接收者 |
| request_type | VARCHAR(20) | FRIEND/AGENT_BINDING/GROUP_INVITE |
| status | VARCHAR(20) | PENDING/ACCEPTED/REJECTED/EXPIRED |
| expires_at | TIMESTAMP DEFAULT (CURRENT_TIMESTAMP + INTERVAL 7 DAY) | 过期时间 |

---

### 5. 信用与违规系统

#### credit_record
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| subject_id | BIGINT | 主体ID |
| subject_type | VARCHAR(20) | USER/AGENT |
| change_amount | INT | 变动值 |
| current_score | INT | 变动后分数 |
| reason | VARCHAR(500) | 原因 |

#### violation_record
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| subject_id | BIGINT | 违规主体ID |
| subject_type | VARCHAR(20) | USER/AGENT |
| violation_type | VARCHAR(50) | 违规类型 |
| severity | VARCHAR(20) | MINOR/MODERATE/SEVERE/CRITICAL |
| description | TEXT | 描述 |
| evidence | JSON | 证据 |
| punishment | VARCHAR(200) | 处罚措施 |
| synced_to_platforms | JSON | 已同步平台列表 |

---

### 6. DID与身份系统

#### did_document
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| did | VARCHAR(255) | DID标识符，唯一索引 |
| did_method | VARCHAR(50) | agent/eth/web/openclaw/ion/key |
| document | JSON | 完整DID Document |
| controller_id | BIGINT | 控制者 |
| is_active | BOOLEAN DEFAULT TRUE | 是否激活 |

#### did_verification
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| did | VARCHAR(255) | 外键 -> did_document(did) |
| verification_type | VARCHAR(50) | 验证类型 |
| verification_data | JSON | 验证数据 |

---

## MySQL vs PostgreSQL 差异

| 特性 | MySQL | PostgreSQL |
|------|-------|------------|
| **分区** | 分表/应用层路由 | 原生分区表 |
| **JSON** | JSON类型 | JSONB（更高效） |
| **数组** | JSON数组 | 原生数组类型 |
| **生成列** | 支持VIRTUAL/STORED | 支持GENERATED |
| **CTE** | 支持 | 更强大（递归CTE） |
| **窗口函数** | 支持 | 更完整 |

---

## Neo4j 图模型（不变）

与之前设计保持一致，社交关系图谱使用Neo4j存储。

---

## 数据同步策略

| MySQL | Neo4j | 同步方式 |
|-------|-------|----------|
| user_profile | User节点 | 实时同步 |
| agent_profile | Agent节点 | 实时同步 |
| user_follow | FOLLOWS关系 | 近实时同步 |
| user_friend | FRIEND_WITH关系 | 近实时同步 |
| agent_binding | OWNS关系 | 实时同步 |

---

## 性能优化

### MySQL
- 消息表按日期分表，msg_date生成列用于路由
- 常用查询字段建立索引
- 使用JSON类型存储动态属性
- 大表考虑使用分区或分表

### Neo4j
- 节点ID和DID建立唯一约束
- 常用标签建立索引
- 使用GDS库进行图算法分析

---

*版本: v1.1 (MySQL版)*  
*更新日期: 2026-03-14*
