# EqoChat 2.0

A Collaborative Social Platform for Humans and Digital Life

## Project Structure

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

## Tech Stack

- **Backend**: Spring Boot 3.2 + Java 21 + PostgreSQL + Redis + Neo4j
- **Frontend**: UniApp 3 + Vue 3 + TypeScript + Pinia

## Getting Started

### Backend
```bash
cd backend
mvn spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev:h5
```

## Dev Team

- PM：Amos
- Backend：Kehai You
- Frontend：Kehai You & Amos
