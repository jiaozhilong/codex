package com.solutionpilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ModelProfileService {
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public ModelProfileService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(15))
        .build();
  }

  public List<Map<String, Object>> listProfiles() {
    return jdbcTemplate.queryForList(
        "select mp.id, mp.name, mp.model_name, mp.api_base, mp.use_for, mp.status, " +
            "mp.api_key_encrypted is not null as api_key_configured, " +
            "p.id as provider_id, p.code as provider_code, p.name as provider_name " +
            "from model_profiles mp join model_providers p on p.id = mp.provider_id order by mp.created_at"
    );
  }

  public List<Map<String, Object>> listProviders() {
    return jdbcTemplate.queryForList("select id, code, name, api_type, enabled from model_providers order by code");
  }

  public Map<String, Object> createProfile(CreateModelProfileCommand command) {
    UUID providerId = jdbcTemplate.queryForObject(
        "select id from model_providers where code = ?",
        UUID.class,
        command.providerCode
    );
    return jdbcTemplate.queryForObject(
        "insert into model_profiles(provider_id, name, model_name, api_base, api_key_encrypted, use_for, status) " +
            "values (?, ?, ?, ?, ?, ?, ?) returning id, name, model_name, api_base, use_for, status",
        (rs, rowNum) -> {
          Map<String, Object> item = new LinkedHashMap<>();
          item.put("id", rs.getObject("id"));
          item.put("name", rs.getString("name"));
          item.put("modelName", rs.getString("model_name"));
          item.put("apiBase", rs.getString("api_base"));
          item.put("useFor", rs.getString("use_for"));
          item.put("status", rs.getString("status"));
          return item;
        },
        providerId,
        command.name,
        command.modelName,
        command.apiBase,
        command.apiKey == null || command.apiKey.isBlank() ? null : "encrypted:" + command.apiKey,
        command.useFor,
        command.status == null ? "PENDING" : command.status
    );
  }

  public void updateProfile(UUID id, CreateModelProfileCommand command) {
    UUID providerId = jdbcTemplate.queryForObject(
        "select id from model_providers where code = ?",
        UUID.class,
        command.providerCode
    );
    jdbcTemplate.update(
        "update model_profiles set provider_id = ?, name = ?, model_name = ?, api_base = ?, " +
            "api_key_encrypted = coalesce(?, api_key_encrypted), use_for = ?, status = ?, updated_at = now() where id = ?",
        providerId,
        command.name,
        command.modelName,
        command.apiBase,
        command.apiKey == null || command.apiKey.isBlank() ? null : "encrypted:" + command.apiKey,
        command.useFor,
        command.status == null ? "PENDING" : command.status,
        id
    );
  }

  public void deleteProfile(UUID id) {
    jdbcTemplate.update("update projects set model_profile_id = null, updated_at = now() where model_profile_id = ?", id);
    jdbcTemplate.update("update agent_runs set model_profile_id = null where model_profile_id = ?", id);
    jdbcTemplate.update("delete from model_profiles where id = ?", id);
  }

  public Map<String, Object> testProfile(UUID id, String prompt) {
    try {
      Map<String, Object> result = chat(id, prompt == null || prompt.isBlank()
          ? "用一句中文说明你已经可以用于 SolutionPilot 方案生成。"
          : prompt);
      jdbcTemplate.update("update model_profiles set status = 'READY', updated_at = now() where id = ?", id);
      return result;
    } catch (Exception ex) {
      jdbcTemplate.update("update model_profiles set status = 'ERROR', updated_at = now() where id = ?", id);
      throw new IllegalStateException("Model test failed: " + ex.getMessage(), ex);
    }
  }

  public Map<String, Object> chat(UUID id, String prompt) {
    Map<String, Object> profile = jdbcTemplate.queryForMap(
        "select mp.id, mp.name, mp.model_name, mp.api_base, mp.api_key_encrypted, p.code as provider_code " +
            "from model_profiles mp join model_providers p on p.id = mp.provider_id where mp.id = ?",
        id
    );
    String apiKey = decrypt((String) profile.get("api_key_encrypted"));
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("Model API Key is empty. Please save API Key first.");
    }
    String apiBase = String.valueOf(profile.get("api_base"));
    String endpoint = chatCompletionsEndpoint(apiBase);
    String userPrompt = prompt == null || prompt.isBlank() ? "请返回 OK。" : prompt;
    try {
      Map<String, Object> message = new LinkedHashMap<>();
      message.put("role", "user");
      message.put("content", userPrompt);

      Map<String, Object> body = new LinkedHashMap<>();
      body.put("model", profile.get("model_name"));
      body.put("messages", List.of(message));
      body.put("temperature", 0.2);
      body.put("max_tokens", 300);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(endpoint))
          .timeout(java.time.Duration.ofSeconds(45))
          .header("Content-Type", "application/json")
          .header("Authorization", "Bearer " + apiKey)
          .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("endpoint", endpoint);
      result.put("httpStatus", response.statusCode());
      result.put("success", response.statusCode() >= 200 && response.statusCode() < 300);
      result.put("prompt", userPrompt);
      result.put("raw", response.body());
      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        JsonNode json = objectMapper.readTree(response.body());
        JsonNode content = json.path("choices").path(0).path("message").path("content");
        result.put("answer", content.isMissingNode() ? response.body() : content.asText());
      } else {
        result.put("answer", response.body());
      }
      return result;
    } catch (Exception ex) {
      throw new IllegalStateException(ex.getMessage(), ex);
    }
  }

  private String chatCompletionsEndpoint(String apiBase) {
    String base = apiBase == null ? "" : apiBase.trim();
    while (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    if (base.endsWith("/chat/completions")) {
      return base;
    }
    return base + "/chat/completions";
  }

  private String decrypt(String value) {
    if (value == null) {
      return null;
    }
    return value.startsWith("encrypted:") ? value.substring("encrypted:".length()) : value;
  }

  public static class CreateModelProfileCommand {
    public String providerCode;
    public String name;
    public String modelName;
    public String apiBase;
    public String apiKey;
    public String useFor;
    public String status;
  }
}
