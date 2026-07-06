package com.solutionpilot.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SkillService {
  private final JdbcTemplate jdbcTemplate;

  public SkillService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Map<String, Object>> listSkills() {
    return jdbcTemplate.queryForList(
        "select id, code, name, description, prompt_template, tool_policy_json::text as tool_policy_json, enabled from agent_skills order by code"
    );
  }

  public void updateSkill(UUID id, SkillCommand command) {
    jdbcTemplate.update(
        "update agent_skills set name = ?, description = ?, prompt_template = ?, tool_policy_json = ?::jsonb, enabled = ?, updated_at = now() where id = ?",
        command.name,
        command.description,
        command.promptTemplate,
        command.toolPolicyJson == null || command.toolPolicyJson.isBlank() ? "{}" : command.toolPolicyJson,
        command.enabled,
        id
    );
  }

  public static class SkillCommand {
    public String name;
    public String description;
    public String promptTemplate;
    public String toolPolicyJson;
    public boolean enabled;
  }
}
