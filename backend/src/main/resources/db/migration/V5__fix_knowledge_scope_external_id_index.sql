drop index if exists ux_knowledge_scopes_external_id;

create unique index if not exists ux_knowledge_scopes_external_id
  on knowledge_scopes(external_id);
