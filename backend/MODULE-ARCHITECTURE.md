# EqoChat Backend - 模块拆分与代码组织指南

## 🎯 核心设计思想

### 1. Server 是空壳
**server 模块只包含**：
- 启动类 `EqoChatApplication.java`
- 配置文件（application.yml 等）

**不包含任何业务逻辑**，仅作为打包和启动的容器。

### 2. 按业务功能拆分 Module
每个独立的业务领域一个 Module：
- `eqochat-user` - 用户相关
- `eqochat-world` - 数字生命世界
- `eqochat-agent` - AI Agent
- `eqochat-chat` - 聊天消息
- `eqochat-contact` - 联系人好友
- `eqochat-project` - 项目管理
- `eqochat-credit` - 信用积分
- `eqochat-notification` - 通知推送

### 3. 每个 Module 包含 API 子模块
```
eqochat-user/
├── eqochat-user-api/    # 对外暴露的接口和 DTO（可被其他模块依赖）
└── eqochat-user/        # 实现细节（禁止被其他模块依赖）
```

**依赖规则**：模块间只能依赖 `-api` 模块，不能依赖实现模块。

### 4. 按子功能细分 Package
不要把所有 Controller 放在一个 package 中，要按子功能继续拆分：

**❌ 错误**：
```
controller/
├── AuthController.java
├── UserController.java
└── ProfileController.java
```

**✅ 正确**：
```
controller/
├── auth/
│   └── AuthController.java
└── profile/
    └── UserController.java
```


---

## 📦 完整的项目结构

```
backend/
│
├── pom.xml  (父工程)
│
├── eqochat-framework/  (技术框架层 - 纯技术组件，无业务逻辑)
│   ├── pom.xml
│   ├── eqochat-framework-common/      # 通用工具、异常、ApiResponse
│   ├── eqochat-framework-redis/       # Redis 封装
│   ├── eqochat-framework-datasource/  # MyBatis-Plus、Druid
│   ├── eqochat-framework-security/    # JWT、Spring Security
│   ├── eqochat-framework-sms/         # 短信发送接口
│   ├── eqochat-framework-websocket/         # websocket 相关
│   └── eqochat-framework-file/        # 文件上传客户端
│
├── eqochat-business/  (业务功能层 - 8 个业务模块)
│   ├── pom.xml
│   │
│   ├── eqochat-user/  (用户模块)
│   │   ├── pom.xml
│   │   ├── eqochat-user-api/  (API 模块 - 可被依赖)
│   │   │   └── src/main/java/com/eqochat/business/user/api/
│   │   │       ├── dto/
│   │   │       │   ├── request/   # 请求 DTO
│   │   │       │   └── response/  # 响应 DTO（给其他模块用的）
│   │   │       └── service/       # Service 接口（不含实现）
│   │   │
│   │   └── eqochat-user/  (实现模块 - 不可被依赖)
│   │       └── src/main/java/com/eqochat/business/user/
│   │           ├── controller/
│   │           │   ├── auth/      # 认证子功能
│   │           │   │   └── AuthController.java
│   │           │   └── profile/   # 个人资料子功能
│   │           │       └── UserController.java
│   │           ├── service/
│   │           │   ├── UserService.java
│   │           │   └── impl/
│   │           │       └── UserServiceImpl.java
│   │           ├── entity/        # 数据库实体
│   │           │   └── User.java
│   │           └── mapper/        # MyBatis Mapper
│   │               └── UserMapper.java
│   │
│   ├── eqochat-world/  (数字生命世界模块)
│   │   ├── eqochat-world-api/
│   │   └── eqochat-world/
│   │       ├── controller/
│   │       │   ├── world/
│   │       │   ├── topic/
│   │       │   └── post/
│   │       ├── service/
│   │       ├── entity/
│   │       └── mapper/
│   │
│   ├── eqochat-agent/  (AI Agent 模块)
│   │   ├── eqochat-agent-api/
│   │   └── eqochat-agent/
│   │
│   ├── eqochat-chat/  (聊天消息模块)
│   │   ├── eqochat-chat-api/
│   │   └── eqochat-chat/
│   │       ├── controller/
│   │       │   ├── message/
│   │       │   └── conversation/
│   │       ├── websocket/  # WebSocket 处理
│   │       ├── service/
│   │       ├── entity/
│   │       └── mapper/
│   │
│   ├── eqochat-contact/  (联系人模块)
│   │   ├── eqochat-contact-api/
│   │   └── eqochat-contact/
│   │       ├── controller/
│   │       │   ├── friend/
│   │       │   ├── group/
│   │       │   └── request/
│   │       └── ...
│   │
│   ├── eqochat-project/  (项目管理模块)
│   │   ├── eqochat-project-api/
│   │   └── eqochat-project/
│   │       ├── controller/
│   │       │   ├── project/
│   │       │   ├── task/
│   │       │   └── member/
│   │       └── ...
│   │
│   ├── eqochat-credit/  (信用积分模块)
│   │   ├── eqochat-credit-api/
│   │   └── eqochat-credit/
│   │
│   └── eqochat-notification/  (通知模块)
│       ├── eqochat-notification-api/
│       └── eqochat-notification/
│
└── eqochat-server/  (启动空壳 - 仅包含启动类和配置)
    ├── pom.xml
    └── src/main/java/com/eqochat/server/
        ├── EqoChatApplication.java
```

---

## 🔧 模块依赖规则

### ✅ 正确的依赖

**场景**：`eqochat-world` 模块需要调用 `eqochat-user` 的功能

```xml
<!-- eqochat-world/pom.xml -->
<dependency>
    <groupId>com.eqochat</groupId>
    <artifactId>eqochat-user-api</artifactId>
    <scope>provided</scope>
</dependency>
```

```java
// eqochat-world 模块中的 Service
@Service
public class WorldPostService {
    
    @Autowired
    private UserProfileApi userProfileApi;  // ✅ 通过 API 接口调用
    
    public WorldPostResp getPostDetail(Long postId) {
        // 通过 API 获取作者信息
        UserProfileResp author = userProfileApi.getUserProfile(authorId);
        // ...
    }
}
```

### ❌ 禁止的依赖

```xml
<!-- 禁止直接依赖实现模块 -->
<dependency>
    <groupId>com.eqochat</groupId>
    <artifactId>eqochat-user</artifactId>
</dependency>
```

```java
// 禁止直接依赖实现类
@Autowired
private UserServiceImpl userService;  // ❌ 错误！
```

---

## 📋 各模块详细内容

### 1. eqochat-user (用户模块)

**API 模块内容** (`eqochat-user-api/`)：
```
api/
├── dto/
│   ├── request/
│   │   ├── LoginReq.java
│   │   ├── RegisterReq.java
│   │   └── UpdateProfileReq.java
│   └── response/
│       ├── LoginResp.java
│       ├── UserProfileResp.java
│       └── UserSearchResp.java
└── service/
    ├── AuthService.java
    └── UserProfileApi.java
```

**实现模块内容** (`eqochat-user/`)：
```
├── controller/
│   ├── auth/
│   │   └── AuthController.java
│   └── profile/
│       └── UserController.java
├── service/
│   ├── UserService.java
│   └── impl/
│       └── UserServiceImpl.java
├── entity/
│   ├── User.java
│   └── UserProfile.java
└── mapper/
    ├── UserMapper.java
    └── UserProfileMapper.java
```

### 2. eqochat-world (数字生命世界模块)

**API 模块**：
```
api/
├── dto/response/
│   ├── WorldResp.java
│   ├── WorldPostResp.java
│   └── WorldTopicResp.java
└── service/
    ├── WorldApi.java
    └── WorldPostApi.java
```

**实现模块**：
```
├── controller/
│   ├── world/
│   │   └── WorldController.java
│   ├── topic/
│   │   └── TopicController.java
│   └── post/
│       └── WorldPostController.java
├── service/
│   └── impl/
├── entity/
│   ├── World.java
│   ├── WorldPost.java
│   └── WorldTopic.java
└── mapper/
```

### 3. eqochat-chat (聊天消息模块)

**API 模块**：
```
api/
├── dto/response/
│   ├── MessageResp.java
│   └── ConversationResp.java
└── service/
    ├── MessageApi.java
    └── ConversationApi.java
```

**实现模块**：
```
├── controller/
│   ├── message/
│   │   └── MessageController.java
│   └── conversation/
│       └── ConversationController.java
├── websocket/
│   ├── WebSocketHandler.java
│   └── WebSocketSessionManager.java
├── service/
├── entity/
│   ├── Message.java
│   └── Conversation.java
└── mapper/
```

### 4. eqochat-contact (联系人模块)

**API 模块**：
```
api/
├── dto/response/
│   ├── ContactResp.java
│   └── FriendResp.java
└── service/
    ├── ContactApi.java
    └── FriendApi.java
```

**实现模块**：
```
├── controller/
│   ├── friend/
│   │   └── FriendController.java
│   ├── group/
│   │   └── GroupController.java
│   └── request/
│       └── FriendRequestController.java
├── service/
├── entity/
│   ├── UserContact.java
│   └── FriendRequest.java
└── mapper/
```

### 5. eqochat-project (项目管理模块)

**API 模块**：
```
api/
├── dto/response/
│   ├── ProjectResp.java
│   └── ProjectTaskResp.java
└── service/
    └── ProjectApi.java
```

**实现模块**：
```
├── controller/
│   ├── project/
│   │   └── ProjectController.java
│   ├── task/
│   │   └── TaskController.java
│   └── member/
│       └── MemberController.java
├── service/
├── entity/
│   ├── Project.java
│   ├── ProjectTask.java
│   └── ProjectMember.java
└── mapper/
```

### 6. eqochat-agent (AI Agent 模块)

**API 模块**：
```
api/
├── dto/response/
│   └── AgentResp.java
└── service/
    └── AgentApi.java
```

**实现模块**：
```
├── controller/
│   └── AgentController.java
├── service/
├── entity/
│   └── Agent.java
└── mapper/
```

### 7. eqochat-credit (信用积分模块)

**API 模块**：
```
api/
├── dto/response/
│   └── CreditResp.java
└── service/
    └── CreditApi.java
```

**实现模块**：
```
├── controller/
│   └── CreditController.java
├── service/
├── entity/
│   └── CreditRecord.java
└── mapper/
```

### 8. eqochat-notification (通知模块)

**API 模块**：
```
api/
├── dto/response/
│   └── NotificationResp.java
└── service/
    └── NotificationApi.java
```

**实现模块**：
```
├── controller/
│   └── NotificationController.java
├── service/
├── entity/
│   └── Notification.java
└── mapper/
```

---

## 🚀 依赖关系图

```
eqochat-server (启动壳)
│
├─> eqochat-user-api
├─> eqochat-world-api
├─> eqochat-agent-api
├─> eqochat-chat-api
├─> eqochat-contact-api
├─> eqochat-project-api
├─> eqochat-credit-api
└─> eqochat-notification-api


eqochat-world (实现模块)
│
├─> eqochat-world-api (自己的 API)
├─> eqochat-user-api (获取用户信息)
├─> eqochat-contact-api (获取联系人信息)
└─> eqochat-framework-common (通用工具)


eqochat-chat (实现模块)
│
├─> eqochat-chat-api (自己的 API)
├─> eqochat-user-api (获取用户信息)
├─> eqochat-contact-api (获取联系人状态)
└─> eqochat-framework-websocket (WebSocket 技术封装)
```

---

## ⚠️ 关键注意事项

### 1. 什么应该放入 API 模块？

✅ **应该放入**：
- Service 接口（不含实现）
- 响应 DTO（返回给其他模块的数据）
- 请求 DTO（其他模块调用时的入参）
- 枚举和常量（其他模块需要知道的状态）

❌ **不应该放入**：
- Controller
- Service 实现类
- Entity（数据库实体）
- Mapper（数据库访问接口）
- 内部 DTO（仅模块内部使用）

### 2. DTO 转换原则

```java
// ✅ 正确：在实现模块内部完成转换
@Service
public class UserServiceImpl implements UserProfileApi {
    
    @Autowired
    private UserMapper mapper;
    
    @Override
    public UserProfileResp getUserProfile(Long userId) {
        // 1. 从数据库获取 Entity
        User user = mapper.selectById(userId);
        
        // 2. Entity -> DTO 转换（在实现模块内部完成）
        UserProfileResp resp = new UserProfileResp();
        resp.setUserId(user.getUserId());
        resp.setNickname(user.getNickname());
        resp.setAvatar(user.getAvatar());
        
        // 3. 返回 DTO（不暴露 Entity）
        return resp;
    }
}
```

### 3. 避免循环依赖

```
❌ 错误：
eqochat-user-api --> eqochat-world-api
eqochat-world-api --> eqochat-user-api

✅ 正确：
单向依赖，或通过事件机制解耦
```

### 4. Package 命名规范

- API 模块：`com.eqochat.business.user.api.*`
- 实现模块：`com.eqochat.business.user.*`
- 子功能 Package：使用单数形式（`auth/`, `profile/`, `message/`）

---

## 📊 迁移步骤

### 步骤 1：迁移 Entity
```bash
# User 相关
mv eqochat-core/entity/User*.java eqochat-business/eqochat-user/eqochat-user/entity/
mv eqochat-core/entity/Agent*.java eqochat-business/eqochat-user/eqochat-user/entity/

# World 相关
mv eqochat-core/entity/World*.java eqochat-business/eqochat-world/eqochat-world/entity/

# ... 其他模块同理
```

### 步骤 2：迁移 DTO 到 API 模块
```bash
# User 相关响应 DTO
mv eqochat-core/dto/response/User*.java eqochat-business/eqochat-user/eqochat-user-api/api/dto/response/

# World 相关响应 DTO
mv eqochat-core/dto/response/World*.java eqochat-business/eqochat-world/eqochat-world-api/api/dto/response/

# ... 其他模块同理
```

### 步骤 3：迁移 Mapper
```bash
mv eqochat-core/mapper/User*.java eqochat-business/eqochat-user/eqochat-user/mapper/
mv eqochat-core/mapper/World*.java eqochat-business/eqochat-world/eqochat-world/mapper/
# ... 其他模块同理
```

### 步骤 4：更新所有 package 名称
```java
// Entity 的 package 更新
package com.eqochat.business.user.entity;

// API 模块的 package 更新
package com.eqochat.business.user.api.dto.response;

// 实现模块的 package 更新
package com.eqochat.business.user.controller.auth;
```

### 步骤 5：更新 server 的 pom.xml
```xml
<dependencies>
    <!-- 依赖所有 API 模块 -->
    <dependency>
        <groupId>com.eqochat</groupId>
        <artifactId>eqochat-user-api</artifactId>
    </dependency>
    <dependency>
        <groupId>com.eqochat</groupId>
        <artifactId>eqochat-world-api</artifactId>
    </dependency>
    <!-- ... 其他 6 个 API 模块 -->
</dependencies>
```

### 步骤 6：验证编译
```bash
mvn clean install
mvn spring-boot:run -pl eqochat-server
```

---

## ✅ 验证清单

- [ ] 所有 Entity 已迁移到对应业务模块
- [ ] 所有 DTO 已迁移到对应 API 模块
- [ ] 所有 Mapper 已迁移到对应业务模块
- [ ] 所有 Controller 按子功能分 package
- [ ] 所有 Service 接口在 API 模块
- [ ] 所有 Service 实现在实现模块
- [ ] server 模块仅包含启动类和 config
- [ ] 模块间依赖都通过 API
- [ ] `mvn clean install` 编译成功
- [ ] 应用可以正常启动

---

## 📞 快速参考

**模块命名**：`eqochat-{业务名}` + `eqochat-{业务名}-api`

**Package 结构**：
- API: `com.eqochat.business.{模块}.api.{子包}`
- 实现：`com.eqochat.business.{模块}.{子包}`

**依赖规则**：只能依赖 `-api` 模块

**子功能划分**：`controller/auth/`, `controller/profile/`

**Core 模块**：最终删除

---

**文档版本**: v2.0
**最后更新**: 2026-04-03
**架构原则**: 清晰的模块边界、松耦合依赖、细粒度组织
