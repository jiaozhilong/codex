alter table agent_skills
  add column if not exists category varchar(40) not null default 'WORKFLOW',
  add column if not exists output_type varchar(40) not null default 'GENERIC',
  add column if not exists sort_order integer not null default 100;

update agent_skills set category = 'WORKFLOW', output_type = 'REQUIREMENT', sort_order = 10 where code = 'requirement';
update agent_skills set category = 'WORKFLOW', output_type = 'PRODUCT', sort_order = 20 where code = 'product';
update agent_skills set category = 'WORKFLOW', output_type = 'CASE', sort_order = 30 where code = 'case';
update agent_skills set category = 'WORKFLOW', output_type = 'ARCHITECTURE', sort_order = 40 where code = 'architecture';
update agent_skills set category = 'WORKFLOW', output_type = 'PROPOSAL', sort_order = 50 where code = 'proposal';
update agent_skills set category = 'WORKFLOW', output_type = 'PPT', sort_order = 60 where code = 'ppt';
update agent_skills set category = 'WORKFLOW', output_type = 'QA', sort_order = 70 where code = 'qa';
update agent_skills set category = 'TOOL', output_type = 'KNOWLEDGE', sort_order = 900 where code = 'ima-skill';
