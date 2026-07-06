# API 设计

Base URL:

```text
/api
```

## Health

### GET /health

返回服务状态。

## Auth

### POST /auth/login

登录。

Request:

```json
{
  "email": "admin@example.com",
  "password": "admin"
}
```

### GET /auth/me

获取当前用户。

## Users

### GET /users

管理员查看用户列表。

### GET /users/roles

查看角色列表。V1 固定为 `ADMIN` 和 `WORKER`，页面显示“管理员”和“牛马专用”。

### POST /users

管理员新增用户。

### PUT /users/{id}/role

管理员调整用户角色。

## Model Profiles

### GET /model-profiles

查询模型配置。

### POST /model-profiles

新增模型配置。

### PUT /model-profiles/{id}

更新模型配置。

### POST /model-profiles/{id}/test

使用已保存的 API Key 调用 OpenAI-compatible `/chat/completions`，验证该模型配置是否可用。

Request:

```json
{
  "prompt": "请用三句话说明自然资源一张图平台的方案价值。"
}
```

## ima Skill

### GET /ima-skill

查看绑定状态、能力开关和知识范围。

### POST /ima-skill/bind

绑定 API Key。

Request:

```json
{
  "apiKey": "ima_skill_key"
}
```

### POST /ima-skill/test-search

测试检索。

V1 会读取已绑定的 API Key，生成可交给支持 ima skill 的模型执行的自然语言调用指令，并尝试向 `solutionpilot.ima.skill-endpoint` 发送 HTTP probe。若 ima 官方未开放稳定 REST search 协议，HTTP probe 不代表最终 skill 调用能力，最终以支持 skill 的模型环境执行结果为准。

Request:

```json
{
  "query": "iPortal 资源授权能力",
  "knowledgeScopeIds": ["..."]
}
```

## Knowledge Scopes

### GET /knowledge-scopes

查询 ima 知识范围。

### POST /knowledge-scopes

新增知识范围。

### PUT /knowledge-scopes/{id}

更新知识范围。

## Projects

### GET /projects

项目列表。

### POST /projects

创建项目。

### GET /projects/{id}

项目详情。

### PUT /projects/{id}

更新项目基本信息、默认模型、ima 知识范围和交付物。

### DELETE /projects/{id}

删除项目。相关知识范围绑定、交付物、工作流运行、Agent 运行和方案资产依赖数据库级联清理。

## Workflow

### POST /projects/{id}/agents/{skillCode}/run

运行指定 Agent。

skillCode:

- `requirement`
- `product`
- `case`
- `architecture`
- `proposal`
- `ppt`
- `qa`
