# SolutionPilot V1

SolutionPilot V1 是面向 GIS 解决方案团队的本地开发版本，已经从静态原型推进为可登录、可管理项目、可配置模型、可绑定 ima Skill、可追踪 Agent 流程的本地 MVP。

## 技术栈

- Backend: Java 11, Spring Boot 2.7.x, Maven
- Runtime: Spring Boot embedded Tomcat for local development; later can package as WAR for external Tomcat if required
- Database: PostgreSQL 15+
- Migration: Flyway
- Persistence: Spring JDBC first, later introduce MyBatis if SQL 复杂度上升
- Auth: Session/JWT compatible design, V1 first uses simple token stub
- AI: OpenAI-compatible `ModelProviderAdapter`
- Knowledge: ima Skill API Key + `KnowledgeService`
- Export: backend service, later add Word/PPT template export
- Frontend: Spring Boot static resources, vanilla HTML/CSS/JS management console

## Local Run

```powershell
cd E:\codex\projects\solution-pilot-v1\backend
$env:JAVA_HOME="D:\java\jdk\jdk11"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn spring-boot:run
```

Default local database:

```text
jdbc:postgresql://localhost:5432/solutionpilot
username: postgres
password: postgis
```

Default server:

```text
http://localhost:8080/api/health
```

Frontend:

```text
http://localhost:8080/api/
```

Login:

```text
admin@example.com / admin
user@example.com / user
```

## V1 Current APIs

- `GET /api/health`
- `GET /api/decisions`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/users`
- `POST /api/users`
- `GET /api/users/roles`
- `PUT /api/users/{id}/role`
- `GET /api/model-providers`
- `GET /api/model-profiles`
- `POST /api/model-profiles`
- `PUT /api/model-profiles/{id}`
- `GET /api/ima-skill`
- `POST /api/ima-skill/bind`
- `POST /api/ima-skill/test-search`
- `GET /api/knowledge-scopes`
- `POST /api/knowledge-scopes`
- `PUT /api/knowledge-scopes/{id}`
- `GET /api/projects`
- `POST /api/projects`
- `GET /api/projects/{id}`
- `POST /api/projects/{projectId}/agents/{skillCode}/run`

## V1 Usable Scope

- 可登录进入前端管理台
- 可按角色控制页面入口：管理员可维护用户、模型、ima、知识范围；牛马专用可处理项目
- 可管理用户，角色为“管理员”和“牛马专用”
- 可查看项目列表、搜索筛选、进入项目工作台、新建/编辑/删除项目
- 可维护模型配置、ima Skill 绑定、ima 知识范围
- 可在项目工作台中单步运行或一键串联运行 mock Agent
- mock Agent 会写入需求分析、产品匹配、案例、架构、方案章节、PPT 页面等业务表

待接入：

- 真实 ima Skill 检索
- 真实大模型调用
- Word/PPT 模板导出

## Decided Scope

- MVP 知识底座只接 ima 知识库。
- 通过 ima Skill API Key 绑定 ima 账号。
- 企业微信知识社区、个人电脑本地文档、网盘资料直连暂不接入。
- 权限只保留管理员和普通用户。
- PPT 按用户提供的 PPTX 模板生成，Agent 只生成结构化页面内容。

## Docs

- [V1 技术决策](./docs/V1_TECH_DECISIONS.md)
- [数据库设计](./docs/DATABASE_DESIGN.md)
- [API 设计](./docs/API_SPEC.md)
- [开发前置清单](./docs/DEV_READINESS.md)
- [V1 当前进度](./docs/V1_PROGRESS.md)
