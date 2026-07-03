package com.solutionpilot.service;

import java.util.List;
import java.util.Map;
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
        "select id, code, name, description, tool_policy_json, enabled from agent_skills order by code"
    );
  }
}
