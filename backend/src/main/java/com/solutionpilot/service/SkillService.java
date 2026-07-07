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
        "select id, code, name, description, category, output_type, sort_order, prompt_template, tool_policy_json::text as tool_policy_json, enabled " +
            "from agent_skills order by category, sort_order, code"
    );
  }

  public Map<String, Object> createSkill(SkillCommand command) {
    return jdbcTemplate.queryForMap(
        "insert into agent_skills(code, name, description, category, output_type, sort_order, prompt_template, tool_policy_json, enabled) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?) returning id, code, name, description, category, output_type, sort_order, prompt_template, tool_policy_json::text as tool_policy_json, enabled",
        normalizeCode(command.code),
        command.name,
        command.description,
        blankDefault(command.category, "WORKFLOW"),
        blankDefault(command.outputType, "GENERIC"),
        command.sortOrder == null ? 100 : command.sortOrder,
        command.promptTemplate,
        command.toolPolicyJson == null || command.toolPolicyJson.isBlank() ? "{}" : command.toolPolicyJson,
        command.enabled
    );
  }

  public void updateSkill(UUID id, SkillCommand command) {
    jdbcTemplate.update(
        "update agent_skills set name = ?, description = ?, category = ?, output_type = ?, sort_order = ?, " +
            "prompt_template = ?, tool_policy_json = ?::jsonb, enabled = ?, updated_at = now() where id = ?",
        command.name,
        command.description,
        blankDefault(command.category, "WORKFLOW"),
        blankDefault(command.outputType, "GENERIC"),
        command.sortOrder == null ? 100 : command.sortOrder,
        command.promptTemplate,
        command.toolPolicyJson == null || command.toolPolicyJson.isBlank() ? "{}" : command.toolPolicyJson,
        command.enabled,
        id
    );
  }

  public void disableSkill(UUID id) {
    jdbcTemplate.update("update agent_skills set enabled = false, updated_at = now() where id = ?", id);
  }

  private String normalizeCode(String code) {
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("任务编码不能为空");
    }
    String normalized = code.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "-").replaceAll("-+", "-");
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("任务编码只能包含字母、数字、中划线或下划线");
    }
    return normalized;
  }

  private String blankDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  public static class SkillCommand {
    public String code;
    public String name;
    public String description;
    public String category;
    public String outputType;
    public Integer sortOrder;
    public String promptTemplate;
    public String toolPolicyJson;
    public boolean enabled;
  }
}
