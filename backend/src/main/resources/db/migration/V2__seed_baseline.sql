insert into roles(code, name) values
  ('ADMIN', '系统管理员'),
  ('USER', '普通用户')
on conflict (code) do nothing;

insert into users(name, email, password_hash, status) values
  ('系统管理员', 'admin@example.com', '{demo}admin', 'ENABLED'),
  ('普通用户', 'user@example.com', '{demo}user', 'ENABLED')
on conflict (email) do nothing;

insert into user_roles(user_id, role_id)
select u.id, r.id
from users u
join roles r on r.code = 'ADMIN'
where u.email = 'admin@example.com'
on conflict do nothing;

insert into user_roles(user_id, role_id)
select u.id, r.id
from users u
join roles r on r.code = 'USER'
where u.email = 'user@example.com'
on conflict do nothing;

insert into model_providers(code, name, api_type, enabled) values
  ('openai', 'OpenAI GPT', 'OPENAI_COMPATIBLE', true),
  ('qwen', '通义千问', 'OPENAI_COMPATIBLE', true),
  ('deepseek', 'DeepSeek', 'OPENAI_COMPATIBLE', true),
  ('local', '本地/私有模型', 'OPENAI_COMPATIBLE', true)
on conflict (code) do nothing;

insert into model_profiles(provider_id, name, model_name, api_base, use_for, status)
select id, '默认 GPT 方案模型', 'gpt-4.1', 'https://api.openai.com/v1', '复杂推理、方案正文、质检', 'PENDING'
from model_providers where code = 'openai';

insert into model_profiles(provider_id, name, model_name, api_base, use_for, status)
select id, '通义千问内网优先', 'qwen-max', 'https://dashscope.aliyuncs.com/compatible-mode/v1', '中文方案、内网部署优先', 'PENDING'
from model_providers where code = 'qwen';

insert into ima_skill_bindings(name, bound_account, status, capabilities_json) values
  ('默认 ima Skill 绑定', null, 'UNBOUND', '{"search": true, "readNote": true, "citation": true, "importMaterial": false, "writeNote": false}'::jsonb);

insert into knowledge_scopes(name, type, owner, scope_prompt, enabled) values
  ('ima 超图官方知识库', '官方产品', 'ima 账号', '优先检索 SuperMap 产品文档、白皮书、能力说明', true),
  ('ima CSDN 技术合集', '技术资料', 'ima 账号', '检索技术文章、部署经验和问题排查资料', true),
  ('ima 历史方案案例库', '方案案例', 'ima 账号', '检索历史项目方案、案例材料和交付模板', true);

insert into agent_skills(code, name, description, prompt_template, tool_policy_json) values
  ('requirement', 'Requirement Agent', '将客户原始需求拆解为结构化需求上下文。', '请基于项目输入提取业务目标、显性需求、隐性需求、风险和待确认问题。', '{"knowledge": false}'::jsonb),
  ('product', 'Product Agent', '调用 ima 知识库匹配产品能力并返回引用。', '请检索 ima 知识库，匹配产品能力，必须返回引用来源和置信度。', '{"knowledge": true, "citationRequired": true}'::jsonb),
  ('case', 'Case Agent', '调用 ima 案例库推荐可参考案例。', '请检索 ima 案例知识范围，返回相似案例、相似原因和可引用内容。', '{"knowledge": true, "citationRequired": true}'::jsonb),
  ('architecture', 'Architecture Agent', '生成业务、应用、数据、技术和部署架构。', '请基于需求和已采纳产品能力生成技术架构，并输出 Mermaid 草图。', '{"knowledge": false}'::jsonb),
  ('proposal', 'Proposal Agent', '生成方案目录和章节正文。', '请基于项目上下文生成方案章节内容，引用产品能力时必须保留来源。', '{"knowledge": true}'::jsonb),
  ('ppt', 'PPT Agent', '生成结构化 PPT 页面内容。', '请按 PPT 模板页类型生成结构化页面内容，不做自由排版。', '{"knowledge": false}'::jsonb),
  ('qa', 'QA Agent', '检查方案覆盖度、引用可靠性和风险。', '请检查方案内容是否覆盖需求、是否缺少引用、是否存在交付风险。', '{"knowledge": false}'::jsonb)
on conflict (code) do nothing;
