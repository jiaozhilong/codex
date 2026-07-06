alter table ima_skill_bindings
  add column if not exists client_id_encrypted text;

alter table knowledge_scopes
  add column if not exists external_id varchar(160),
  add column if not exists source varchar(80) not null default 'MANUAL',
  add column if not exists cover_url varchar(800);

create unique index if not exists ux_knowledge_scopes_external_id
  on knowledge_scopes(external_id)
  where external_id is not null;

create table if not exists ima_knowledge_bases (
  id uuid primary key default gen_random_uuid(),
  knowledge_base_id varchar(160) not null unique,
  name varchar(240) not null,
  cover_url varchar(800),
  can_add boolean not null default false,
  source_type varchar(80) not null default 'SUBSCRIBED',
  raw_json jsonb not null default '{}'::jsonb,
  synced_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

insert into agent_skills(code, name, description, prompt_template, tool_policy_json, enabled) values
  (
    'ima-skill',
    'ima Skill',
    'IMA OpenAPI 工具技能，负责查询账号知识库、检索知识库内容、读取笔记和返回引用。',
    '使用 ima OpenAPI。查看账号知识库列表时调用 search_knowledge_base 空 query；检索知识库内容时调用 search_knowledge；需要添加内容时先调用 get_addable_knowledge_base_list。',
    '{"tool": "ima-openapi", "knowledge": true, "apis": ["search_knowledge_base", "get_addable_knowledge_base_list", "get_knowledge_base", "get_knowledge_list", "search_knowledge"], "citationRequired": true}'::jsonb,
    true
  )
on conflict (code) do update set
  name = excluded.name,
  description = excluded.description,
  prompt_template = excluded.prompt_template,
  tool_policy_json = excluded.tool_policy_json,
  enabled = true,
  updated_at = now();
