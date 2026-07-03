package com.solutionpilot.service;

import com.solutionpilot.config.ImaSkillProperties;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ImaSkillService {
  private final ImaSkillProperties properties;
  private final JdbcTemplate jdbcTemplate;

  public ImaSkillService(ImaSkillProperties properties, JdbcTemplate jdbcTemplate) {
    this.properties = properties;
    this.jdbcTemplate = jdbcTemplate;
  }

  public Map<String, Object> status() {
    Map<String, Object> binding = jdbcTemplate.queryForMap(
        "select id, name, bound_account, status, capabilities_json from ima_skill_bindings order by created_at limit 1"
    );
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("endpoint", properties.getSkillEndpoint());
    payload.put("binding", binding);
    payload.put("bound", "BOUND".equals(binding.get("status")) || (properties.getSkillKey() != null && !properties.getSkillKey().isBlank()));
    payload.put("knowledgeBase", "ima knowledge base");
    payload.put("disabledSources", "wecom knowledge community, local desktop documents, cloud drive direct access");
    payload.put("capabilities", capabilities());
    return payload;
  }

  public Map<String, Object> bind(String apiKey, String boundAccount) {
    jdbcTemplate.update(
        "update ima_skill_bindings set api_key_encrypted = ?, bound_account = ?, status = 'BOUND', updated_at = now() " +
            "where id = (select id from ima_skill_bindings order by created_at limit 1)",
        "encrypted:" + apiKey,
        boundAccount == null || boundAccount.isBlank() ? "ima account" : boundAccount
    );
    return status();
  }

  public Map<String, Object> testSearch(String query) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("query", query);
    payload.put("provider", "ima_skill");
    payload.put("status", "mocked");
    payload.put("time", OffsetDateTime.now());
    payload.put("summary", "V1 placeholder: KnowledgeService will translate the query into an ima Skill instruction.");
    payload.put("citations", Arrays.asList("ima: 超图官方知识库 / 示例来源", "ima: 历史方案案例库 / 示例来源"));
    return payload;
  }

  private List<String> capabilities() {
    return Arrays.asList("searchKnowledge", "readNote", "returnCitations");
  }
}
