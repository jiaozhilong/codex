# 数据库设计

## 1. 设计原则

- `projects` 是主业务实体。
- Agent 每次运行必须可追溯。
- 知识引用必须可追溯到来源。
- API Key 等敏感字段只保存加密值。
- V1 权限保持简单，避免过早设计复杂组织架构。

## 2. 核心表

### users

系统用户。

- `id`
- `name`
- `email`
- `password_hash`
- `status`
- `created_at`
- `updated_at`

### roles

V1 固定两类：`ADMIN`、`WORKER`。页面展示为“管理员”和“牛马专用”。

- `id`
- `code`
- `name`

### user_roles

用户角色关系。

- `user_id`
- `role_id`

### model_providers

模型供应商。

- `id`
- `code`
- `name`
- `api_type`
- `enabled`

### model_profiles

具体模型配置。

- `id`
- `provider_id`
- `name`
- `model_name`
- `api_base`
- `api_key_encrypted`
- `use_for`
- `status`
- `created_at`
- `updated_at`

### ima_skill_bindings

ima Skill 账号绑定。

- `id`
- `name`
- `bound_account`
- `api_key_encrypted`
- `status`
- `capabilities_json`
- `created_at`
- `updated_at`

### knowledge_scopes

ima 知识范围，不直接建企业微信或本地文档来源。

- `id`
- `name`
- `type`
- `owner`
- `scope_prompt`
- `enabled`
- `last_verified_at`
- `created_at`
- `updated_at`

### projects

项目主表。

- `id`
- `name`
- `customer_name`
- `industry`
- `customer_type`
- `background`
- `raw_demand`
- `existing_systems`
- `budget`
- `delivery_time`
- `status`
- `progress`
- `creator_id`
- `model_profile_id`
- `created_at`
- `updated_at`

### project_knowledge_scopes

项目选择的 ima 知识范围。

- `project_id`
- `knowledge_scope_id`

### project_deliverables

项目交付物。

- `id`
- `project_id`
- `deliverable_type`
- `enabled`

### agent_skills

配置化 skills。

- `id`
- `code`
- `name`
- `description`
- `input_schema_json`
- `output_schema_json`
- `prompt_template`
- `tool_policy_json`
- `enabled`
- `created_at`
- `updated_at`

### workflow_runs

一次项目工作流运行。

- `id`
- `project_id`
- `status`
- `started_by`
- `started_at`
- `finished_at`

### agent_runs

单个 Agent 运行记录。

- `id`
- `workflow_run_id`
- `project_id`
- `skill_id`
- `model_profile_id`
- `status`
- `input_json`
- `output_json`
- `error_message`
- `started_at`
- `finished_at`

### knowledge_cache

ima 检索缓存。

- `id`
- `query_hash`
- `query_text`
- `knowledge_scope_ids`
- `result_json`
- `expires_at`
- `created_at`

### citations

引用来源。

- `id`
- `project_id`
- `agent_run_id`
- `source_title`
- `source_uri`
- `source_type`
- `snippet`
- `confidence`
- `created_at`

### requirement_analyses

需求分析结果。

- `id`
- `project_id`
- `agent_run_id`
- `content_json`
- `created_at`

### product_matches

产品匹配结果。

- `id`
- `project_id`
- `agent_run_id`
- `requirement_text`
- `product_name`
- `module_name`
- `capability`
- `confidence`
- `adopted`
- `citation_id`
- `created_at`

### case_matches

案例推荐结果。

- `id`
- `project_id`
- `agent_run_id`
- `case_name`
- `industry`
- `similarity_reason`
- `reference_content`
- `adopted`
- `citation_id`
- `created_at`

### architectures

技术架构结果。

- `id`
- `project_id`
- `agent_run_id`
- `content_json`
- `mermaid_text`
- `created_at`

### proposal_sections

方案章节。

- `id`
- `project_id`
- `title`
- `sort_order`
- `content_markdown`
- `status`
- `created_at`
- `updated_at`

### ppt_pages

PPT 结构化页面。

- `id`
- `project_id`
- `page_type`
- `title`
- `content_json`
- `sort_order`
- `created_at`
- `updated_at`

### export_jobs

导出任务。

- `id`
- `project_id`
- `export_type`
- `template_id`
- `status`
- `file_path`
- `error_message`
- `created_at`
- `finished_at`

### audit_logs

审计日志。

- `id`
- `actor_id`
- `action`
- `target_type`
- `target_id`
- `detail_json`
- `created_at`

## 3. V1 建表策略

第一批迁移必须包含：

- users / roles / user_roles
- model_providers / model_profiles
- ima_skill_bindings / knowledge_scopes
- projects / project_knowledge_scopes / project_deliverables
- agent_skills / workflow_runs / agent_runs
- knowledge_cache / citations

第二批再补：

- proposal_sections
- ppt_pages
- export_jobs
- audit_logs
