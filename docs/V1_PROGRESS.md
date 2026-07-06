# V1 当前进度

## 已完成

- 本地 PostgreSQL 连接：`postgres / postgis`
- 数据库：Flyway 已可自动建表和插入 baseline 数据
- 基础接口：
  - health
  - decisions
  - auth login / me
  - users
  - model providers / profiles
  - ima Skill status / bind / test-search
  - knowledge scopes
  - projects
  - skills
  - mock agent run
- 前端管理台：
  - 登录页
  - 总览
  - 项目工作台
  - 用户管理
  - 模型配置
  - ima Skill
  - 知识范围
- 配置联调：
  - 大模型配置支持 OpenAI-compatible `/chat/completions` 实际调用测试
  - ima Skill 支持账号/API Key 保存、调用指令生成和 endpoint probe
- 角色：
  - 管理员
  - 牛马专用
- 项目创建：
  - 写入 `projects`
  - 绑定默认模型
  - 默认绑定全部启用的 ima 知识范围
  - 写入交付物
- 项目基础 CRUD：
  - 项目列表、搜索和筛选
  - 新建项目
  - 编辑项目基本信息、模型、知识范围和交付物
  - 删除项目并级联清理关联资产
- 项目工作台：
  - 单步运行 Agent
  - 一键串联运行需求分析、产品匹配、案例推荐、架构设计、方案章节、PPT 页面、方案质检
  - 查看各类 Agent 产出
- Agent mock 落表：
  - Requirement Agent -> `requirement_analyses`
  - Product Agent -> `product_matches` + `citations`
  - Case Agent -> `case_matches` + `citations`
  - Architecture Agent -> `architectures`
  - Proposal Agent -> `proposal_sections`
  - PPT Agent -> `ppt_pages`
  - 每次运行均写入 `workflow_runs` / `agent_runs`

## 已验证

本地 PostgreSQL 版本：

```text
PostgreSQL 14.8
```

Flyway 迁移：

```text
V1 init schema: success
V2 seed baseline: success
V3 rename USER to WORKER / 牛马专用: success
```

完整 V1 冒烟结果：

```json
{
  "health": "ok",
  "loginUser": "系统管理员",
  "roleCodes": "ADMIN,WORKER",
  "modelCount": 2,
  "projectCreate": "success",
  "projectUpdate": "success",
  "projectDelete": "success",
  "agentRunStatus": "COMPLETED",
  "agentRuns": 7,
  "requirementAnalyses": 1,
  "productMatches": 1,
  "caseMatches": 1,
  "architectures": 1,
  "proposalSections": 2,
  "pptPages": 2,
  "modelConfigTest": "success with local OpenAI-compatible mock",
  "imaConfigTest": "configured + endpoint probe attempted",
  "htmlOk": true
}
```

## 下一步

- 把 mock Agent 输出替换为 `SkillRegistry + ModelProviderAdapter`
- Product Agent 接入已配置模型和 `KnowledgeService -> ima Skill`
- 增加 Word/PPT 导出任务骨架
