# V1 技术决策

## 1. 本地开发形态

V1 使用 Java + Spring Boot 作为主后端。你本地有 IDEA、VSCode 和 Tomcat，因此优先保证 IDEA 可直接打开 Maven 工程、命令行可运行、后续也能部署到 Tomcat。

默认运行方式：

- 开发期：`mvn spring-boot:run`
- 本地调试：IDEA 直接运行 `SolutionPilotApplication`
- 后续部署：优先 jar；如果必须部署外部 Tomcat，再切换 war packaging

## 2. 技术栈定版

- Java: 11
- Spring Boot: 2.7.18
- Build: Maven
- Web: Spring MVC
- DB: PostgreSQL 15+
- Migration: Flyway
- Persistence: Spring JDBC
- Config: application.yml + environment variables
- Auth: V1 简化 token，V2 再接 SSO
- Frontend: 原型继续作为 UI 参考，V1 先交付 REST 后端

## 3. 知识库定版

- 主知识来源：ima 知识库
- 接入方式：ima Skill API Key
- 管理对象：绑定状态、能力开关、知识范围、调用测试
- 暂不接入：企业微信知识社区、个人电脑文档、网盘资料直连
- 原因：这些来源权限边界和同步成本高，不适合 V1

## 4. Agent Skills 定版

V1 不训练模型，采用配置化 skills。

每个 skill 由以下部分组成：

- 输入 schema
- 输出 schema
- prompt 模板
- 可用工具
- 知识库范围
- 审核规则

V1 内置 skills：

- Requirement Agent
- Product Agent
- Case Agent
- Architecture Agent
- Proposal Agent
- PPT Agent
- QA Agent

## 5. Export 定版

- Word: 后端按 Markdown/结构化章节生成 docx
- PPT: 用户提供标准 pptx 模板，后端按页面类型填充
- V1 先保存结构化 PPT 页面数据，模板渲染放到 V1.1

## 6. 权限定版

- 管理员：用户、模型、ima Skill、知识范围、系统配置
- 牛马专用：创建项目、运行 Agent、查看项目、导出成果

复杂团队、部门、审核流放到 V2。
