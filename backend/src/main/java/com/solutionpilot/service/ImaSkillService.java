package com.solutionpilot.service;

import com.solutionpilot.config.ImaSkillProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public ImaSkillService(ImaSkillProperties properties, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.properties = properties;
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(15))
        .build();
  }

  public Map<String, Object> status() {
    Map<String, Object> binding = jdbcTemplate.queryForMap(
        "select id, name, bound_account, status, capabilities_json, api_key_encrypted, updated_at from ima_skill_bindings order by created_at limit 1"
    );
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("endpoint", properties.getSkillEndpoint());
    payload.put("binding", safeBinding(binding));
    payload.put("bound", hasApiKey(binding));
    payload.put("knowledgeBase", "ima knowledge base");
    payload.put("disabledSources", "wecom knowledge community, local desktop documents, cloud drive direct access");
    payload.put("integrationMode", "ima Skill API Key. Public docs describe binding the key to an ima account and invoking the skill from a model/tool environment; no stable public REST search contract is documented.");
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
    Map<String, Object> binding = jdbcTemplate.queryForMap(
        "select id, name, bound_account, status, api_key_encrypted from ima_skill_bindings order by created_at limit 1"
    );
    String apiKey = decrypt((String) binding.get("api_key_encrypted"));
    if ((apiKey == null || apiKey.isBlank()) && properties.getSkillKey() != null && !properties.getSkillKey().isBlank()) {
      apiKey = properties.getSkillKey();
    }
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("ima Skill API Key is empty. Please bind API Key first.");
    }
    String instruction = "请调用已绑定的 ima 知识库，检索：" + query + "。返回摘要、引用来源、相关笔记或知识库条目。";
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("query", query);
    payload.put("provider", "ima_skill");
    payload.put("status", "CONFIGURED");
    payload.put("time", OffsetDateTime.now());
    payload.put("boundAccount", binding.get("bound_account"));
    payload.put("instruction", instruction);
    payload.put("summary", "ima Skill API Key 已保存。系统已生成可交给支持 skill 的模型执行的 ima 调用指令，并尝试检测 ima endpoint。");
    payload.put("citations", Arrays.asList("ima skill call will return citations when executed by the bound skill-enabled model"));
    payload.put("httpProbe", probeEndpoint(apiKey, query, instruction));
    return payload;
  }

  private Map<String, Object> probeEndpoint(String apiKey, String query, String instruction) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("query", query);
    body.put("instruction", instruction);
    body.put("capability", "searchKnowledge");
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("endpoint", properties.getSkillEndpoint());
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(properties.getSkillEndpoint()))
          .timeout(java.time.Duration.ofSeconds(20))
          .header("Content-Type", "application/json")
          .header("Authorization", "Bearer " + apiKey)
          .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      result.put("attempted", true);
      result.put("httpStatus", response.statusCode());
      result.put("success", response.statusCode() >= 200 && response.statusCode() < 300);
      result.put("raw", response.body());
      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        jdbcTemplate.update("update ima_skill_bindings set status = 'BOUND', updated_at = now() where id = (select id from ima_skill_bindings order by created_at limit 1)");
      }
    } catch (Exception ex) {
      result.put("attempted", true);
      result.put("success", false);
      result.put("error", ex.getMessage());
      result.put("note", "If ima does not expose a REST search endpoint, this is expected. Use the generated instruction in a model environment with ima skill enabled.");
    }
    return result;
  }

  private boolean hasApiKey(Map<String, Object> binding) {
    return "BOUND".equals(binding.get("status"))
        || decrypt((String) binding.get("api_key_encrypted")) != null
        || (properties.getSkillKey() != null && !properties.getSkillKey().isBlank());
  }

  private Map<String, Object> safeBinding(Map<String, Object> binding) {
    Map<String, Object> safe = new LinkedHashMap<>(binding);
    String key = decrypt((String) binding.get("api_key_encrypted"));
    safe.remove("api_key_encrypted");
    safe.put("apiKeyConfigured", key != null && !key.isBlank());
    safe.put("apiKeyMasked", mask(key));
    return safe;
  }

  private String decrypt(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.startsWith("encrypted:") ? value.substring("encrypted:".length()) : value;
  }

  private String mask(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    if (value.length() <= 8) {
      return "****";
    }
    return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
  }

  private List<String> capabilities() {
    return Arrays.asList("searchKnowledge", "readNote", "returnCitations");
  }
}
