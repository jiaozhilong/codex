create extension if not exists pgcrypto;

create table users (
  id uuid primary key default gen_random_uuid(),
  name varchar(100) not null,
  email varchar(200) not null unique,
  password_hash varchar(255),
  status varchar(30) not null default 'ENABLED',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table roles (
  id uuid primary key default gen_random_uuid(),
  code varchar(50) not null unique,
  name varchar(100) not null
);

create table user_roles (
  user_id uuid not null references users(id) on delete cascade,
  role_id uuid not null references roles(id) on delete cascade,
  primary key (user_id, role_id)
);

create table model_providers (
  id uuid primary key default gen_random_uuid(),
  code varchar(50) not null unique,
  name varchar(100) not null,
  api_type varchar(50) not null default 'OPENAI_COMPATIBLE',
  enabled boolean not null default true
);

create table model_profiles (
  id uuid primary key default gen_random_uuid(),
  provider_id uuid not null references model_providers(id),
  name varchar(120) not null,
  model_name varchar(120) not null,
  api_base varchar(500) not null,
  api_key_encrypted text,
  use_for varchar(500),
  status varchar(30) not null default 'PENDING',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table ima_skill_bindings (
  id uuid primary key default gen_random_uuid(),
  name varchar(120) not null,
  bound_account varchar(200),
  api_key_encrypted text,
  status varchar(30) not null default 'UNBOUND',
  capabilities_json jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table knowledge_scopes (
  id uuid primary key default gen_random_uuid(),
  name varchar(160) not null,
  type varchar(80) not null,
  owner varchar(120) not null default 'ima account',
  scope_prompt text not null,
  enabled boolean not null default true,
  last_verified_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table projects (
  id uuid primary key default gen_random_uuid(),
  name varchar(200) not null,
  customer_name varchar(200) not null,
  industry varchar(80),
  customer_type varchar(80),
  background text,
  raw_demand text,
  existing_systems text,
  budget varchar(120),
  delivery_time varchar(120),
  status varchar(40) not null default 'DRAFT',
  progress integer not null default 0,
  creator_id uuid references users(id),
  model_profile_id uuid references model_profiles(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table project_knowledge_scopes (
  project_id uuid not null references projects(id) on delete cascade,
  knowledge_scope_id uuid not null references knowledge_scopes(id),
  primary key (project_id, knowledge_scope_id)
);

create table project_deliverables (
  id uuid primary key default gen_random_uuid(),
  project_id uuid not null references projects(id) on delete cascade,
  deliverable_type varchar(80) not null,
  enabled boolean not null default true
);

create table agent_skills (
  id uuid primary key default gen_random_uuid(),
  code varchar(80) not null unique,
  name varchar(120) not null,
  description text,
  input_schema_json jsonb not null default '{}'::jsonb,
  output_schema_json jsonb not null default '{}'::jsonb,
  prompt_template text not null,
  tool_policy_json jsonb not null default '{}'::jsonb,
  enabled boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table workflow_runs (
  id uuid primary key default gen_random_uuid(),
  project_id uuid not null references projects(id) on delete cascade,
  status varchar(40) not null default 'RUNNING',
  started_by uuid references users(id),
  started_at timestamptz not null default now(),
  finished_at timestamptz
);

create table agent_runs (
  id uuid primary key default gen_random_uuid(),
  workflow_run_id uuid references workflow_runs(id) on delete set null,
  project_id uuid not null references projects(id) on delete cascade,
  skill_id uuid not null references agent_skills(id),
  model_profile_id uuid references model_profiles(id),
  status varchar(40) not null default 'PENDING',
  input_json jsonb not null default '{}'::jsonb,
  output_json jsonb not null default '{}'::jsonb,
  error_message text,
  started_at timestamptz,
  finished_at timestamptz
);

create table knowledge_cache (
  id uuid primary key default gen_random_uuid(),
  query_hash varchar(128) not null unique,
  query_text text not null,
  knowledge_scope_ids text not null,
  result_json jsonb not null default '{}'::jsonb,
  expires_at timestamptz,
  created_at timestamptz not null default now()
);

create table citations (
  id uuid primary key default gen_random_uuid(),
  project_id uuid not null references projects(id) on delete cascade,
  agent_run_id uuid references agent_runs(id) on delete set null,
  source_title varchar(300) not null,
  source_uri varchar(800),
  source_type varchar(80) not null default 'IMA',
  snippet text,
  confidence numeric(5, 4),
  created_at timestamptz not null default now()
);

create table requirement_analyses (
  id uuid primary key default gen_random_uuid(),
  project_id uuid not null references projects(id) on delete cascade,
  agent_run_id uuid references agent_runs(id) on delete set null,
  content_json jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create table product_matches (
  id uuid primary key default gen_random_uuid(),
  project_id uuid not null references projects(id) on delete cascade,
  agent_run_id uuid references agent_runs(id) on delete set null,
  requirement_text text not null,
  product_name varchar(160) not null,
  module_name varchar(160),
  capability text not null,
  confidence numeric(5, 4),
  adopted boolean not null default false,
  citation_id uuid references citations(id) on delete set null,
  created_at timestamptz not null default now()
);

create table case_matches (
  id uuid primary key default gen_random_uuid(),
  project_id uuid not null references projects(id) on delete cascade,
  agent_run_id uuid references agent_runs(id) on delete set null,
  case_name varchar(240) not null,
  industry varchar(80),
  similarity_reason text,
  reference_content text,
  adopted boolean not null default false,
  citation_id uuid references citations(id) on delete set null,
  created_at timestamptz not null default now()
);

create table architectures (
  id uuid primary key default gen_random_uuid(),
  project_id uuid not null references projects(id) on delete cascade,
  agent_run_id uuid references agent_runs(id) on delete set null,
  content_json jsonb not null default '{}'::jsonb,
  mermaid_text text,
  created_at timestamptz not null default now()
);

create table proposal_sections (
  id uuid primary key default gen_random_uuid(),
  project_id uuid not null references projects(id) on delete cascade,
  title varchar(200) not null,
  sort_order integer not null,
  content_markdown text,
  status varchar(40) not null default 'DRAFT',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table ppt_pages (
  id uuid primary key default gen_random_uuid(),
  project_id uuid not null references projects(id) on delete cascade,
  page_type varchar(80) not null,
  title varchar(200) not null,
  content_json jsonb not null default '{}'::jsonb,
  sort_order integer not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table export_jobs (
  id uuid primary key default gen_random_uuid(),
  project_id uuid not null references projects(id) on delete cascade,
  export_type varchar(50) not null,
  template_id uuid,
  status varchar(40) not null default 'PENDING',
  file_path varchar(800),
  error_message text,
  created_at timestamptz not null default now(),
  finished_at timestamptz
);

create table audit_logs (
  id uuid primary key default gen_random_uuid(),
  actor_id uuid references users(id) on delete set null,
  action varchar(120) not null,
  target_type varchar(120),
  target_id uuid,
  detail_json jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create index idx_projects_creator on projects(creator_id);
create index idx_agent_runs_project on agent_runs(project_id);
create index idx_citations_project on citations(project_id);
create index idx_product_matches_project on product_matches(project_id);
create index idx_case_matches_project on case_matches(project_id);
