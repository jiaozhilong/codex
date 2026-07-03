package com.solutionpilot.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeScopeService {
  private final JdbcTemplate jdbcTemplate;

  public KnowledgeScopeService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Map<String, Object>> listScopes() {
    return jdbcTemplate.queryForList(
        "select id, name, type, owner, scope_prompt, enabled, last_verified_at from knowledge_scopes order by created_at"
    );
  }

  public Map<String, Object> createScope(KnowledgeScopeCommand command) {
    return jdbcTemplate.queryForObject(
        "insert into knowledge_scopes(name, type, owner, scope_prompt, enabled) values (?, ?, ?, ?, ?) " +
            "returning id, name, type, owner, scope_prompt, enabled",
        (rs, rowNum) -> {
          Map<String, Object> item = new LinkedHashMap<>();
          item.put("id", rs.getObject("id"));
          item.put("name", rs.getString("name"));
          item.put("type", rs.getString("type"));
          item.put("owner", rs.getString("owner"));
          item.put("scopePrompt", rs.getString("scope_prompt"));
          item.put("enabled", rs.getBoolean("enabled"));
          return item;
        },
        command.name,
        command.type,
        command.owner == null ? "ima 账号" : command.owner,
        command.scopePrompt,
        command.enabled
    );
  }

  public void updateScope(UUID id, KnowledgeScopeCommand command) {
    jdbcTemplate.update(
        "update knowledge_scopes set name = ?, type = ?, owner = ?, scope_prompt = ?, enabled = ?, updated_at = now() where id = ?",
        command.name,
        command.type,
        command.owner == null ? "ima 账号" : command.owner,
        command.scopePrompt,
        command.enabled,
        id
    );
  }

  public static class KnowledgeScopeCommand {
    public String name;
    public String type;
    public String owner;
    public String scopePrompt;
    public boolean enabled = true;
  }
}
