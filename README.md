# EqoChat 2.0

人与数字生命协同社交平台

## 项目结构

```
EqoChat2.0/
├── backend/          # Spring Boot 后端
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/eqochat/
│           │   ├── EqoChatApplication.java
│           │   ├── common/
│           │   ├── config/
│           │   ├── controller/
│           │   ├── domain/
│           │   └── ...
│           └── resources/
│               ├── application.yml
│               └── db/migration/
├── frontend/         # UniApp 前端
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── pages/
│       ├── components/
│       ├── stores/
│       ├── api/
│       └── ...
└── README.md
```

## 技术栈

- **后端**: Spring Boot 3.2 + Java 21 + PostgreSQL + Redis + Neo4j
- **前端**: UniApp 3 + Vue 3 + TypeScript + Pinia

## 启动项目

### 后端
```bash
cd backend
mvn spring-boot:run
```

### 前端
```bash
cd frontend
npm install
npm run dev:h5
```

## 开发团队

- 产品：Amos
- 后端：游克海
- 前端：开发中
