# 开发前置清单

## 本地软件

- JDK 11
- Maven 3.6+
- PostgreSQL 15+
- IDEA
- VSCode
- Tomcat 可选，V1 默认使用 Spring Boot embedded Tomcat

## 环境变量

```text
JAVA_HOME=D:\java\jdk\jdk11
SOLUTIONPILOT_DB_URL=jdbc:postgresql://localhost:5432/solutionpilot
SOLUTIONPILOT_DB_USERNAME=postgres
SOLUTIONPILOT_DB_PASSWORD=postgis
SOLUTIONPILOT_IMA_SKILL_KEY=
```

Windows PowerShell 临时设置：

```powershell
$env:JAVA_HOME="D:\java\jdk\jdk11"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

## 本地数据库

建议创建：

```sql
create database solutionpilot;
-- 本地 V1 默认使用 postgres / postgis 连接。
```

也可以直接参考：

```text
E:\codex\projects\solution-pilot-v1\docs\local-postgres-init.sql
```

## V1 开发顺序

1. 后端工程启动和健康检查
2. Flyway 建表
3. 用户和权限 seed 数据
4. 模型配置 API
5. ima Skill 配置 API
6. 项目创建 API
7. Agent run 数据结构和 mock 运行
8. KnowledgeService 接 ima Skill
9. 导出任务骨架

## 验收口径

- 能用 IDEA 启动后端。
- 能连本地 PostgreSQL 并自动建表。
- 能创建项目并选择 ima 知识范围。
- 能保存模型配置和 ima Skill 绑定状态。
- 能跑 mock Agent 并记录 agent_runs。
- 后续替换真实模型和 ima Skill 时不改项目主流程。

## 接口冒烟

VSCode REST Client 或 IDEA HTTP Client 可以直接打开：

```text
E:\codex\projects\solution-pilot-v1\docs\api-smoke.http
```
