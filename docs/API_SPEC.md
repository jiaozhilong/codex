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
