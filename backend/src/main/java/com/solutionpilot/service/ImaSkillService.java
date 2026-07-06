package com.solutionpilot.service;

import com.solutionpilot.config.ImaSkillProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
  private static final String IMA_OPENAPI_BASE = "https://ima.qq.com";
  private static final String IMA_SKILL_VERSION = "1.1.7";

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
        "select id, name, bound_account, status, capabilities_json, api_key_encrypted, client_id_encrypted, updated_at from ima_skill_bindings order by created_at limit 1"
    );
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("endpoint", IMA_OPENAPI_BASE + "/openapi/wiki/v1");
    payload.put("binding", safeBinding(binding));
    payload.put("bound", hasApiKey(binding));
    payload.put("knowledgeBase", "ima knowledge base");
    payload.put("disabledSources", "wecom knowledge community, local desktop documents, cloud drive direct access");
    payload.put("integrationMode", "ima Skill OpenAPI. 使用 Client ID + API Key 调用 ima OpenAPI，并将账号知识库同步到系统。");
    payload.put("capabilities", capabilities());
    payload.put("subscriptionSource", "ima OpenAPI search_knowledge_base 同步结果，并自动映射为项目知识范围。");
    payload.put("remoteSubscriptionStatus", remoteSubscriptionStatus());
    payload.put("subscriptions", subscriptions());
    payload.put("syncSummary", syncSummary());
    return payload;
  }

  public List<Map<String, Object>> subscriptions() {
    List<Map<String, Object>> remote = jdbcTemplate.queryForList(
        "select knowledge_base_id as id, name, 'ima知识库' as type, 'ima账号' as owner, " +
            "case when can_add then '可检索 / 可添加' else '可检索' end as scope_prompt, true as enabled, " +
            "cover_url, can_add, source_type, raw_json->>'content_count' as content_count, " +
            "raw_json->>'member_count' as member_count, raw_json->>'role_type' as role_type, " +
            "raw_json->>'base_type' as base_type, synced_at, updated_at from ima_knowledge_bases order by name"
    );
    for (Map<String, Object> item : remote) {
      item.put("source", "ima_openapi");
      item.put("sourceLabel", "ima账号订阅库");
      item.put("status", "AVAILABLE");
    }
    if (!remote.isEmpty()) {
      return remote;
    }
    List<Map<String, Object>> local = jdbcTemplate.queryForList(
        "select id, name, type, owner, scope_prompt, enabled, last_verified_at, updated_at from knowledge_scopes order by enabled desc, name"
    );
    for (Map<String, Object> scope : local) {
      scope.put("source", "local_scope");
      scope.put("sourceLabel", "本地知识范围");
      scope.put("status", Boolean.TRUE.equals(scope.get("enabled")) ? "AVAILABLE" : "DISABLED");
    }
    return local;
  }

  private Map<String, Object> remoteSubscriptionStatus() {
    Map<String, Object> status = new LinkedHashMap<>();
    boolean hasCredential = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
        "select count(*) > 0 from ima_skill_bindings where api_key_encrypted is not null and client_id_encrypted is not null",
        Boolean.class
    ));
    Integer synced = jdbcTemplate.queryForObject("select count(*) from ima_knowledge_bases", Integer.class);
    status.put("readable", hasCredential);
    status.put("endpoint", IMA_OPENAPI_BASE + "/openapi/wiki/v1/search_knowledge_base");
    status.put("message", hasCredential ? "已配置 Client ID 和 API Key，可同步账号订阅知识库。" : "请先配置 ima Client ID 和 API Key。");
    status.put("syncedCount", synced == null ? 0 : synced);
    status.put("nextStep", "点击同步订阅库，系统会调用 search_knowledge_base 空 query 获取账号知识库列表。");
    return status;
  }

  public Map<String, Object> bind(String apiKey, String clientId, String boundAccount) {
    jdbcTemplate.update(
        "update ima_skill_bindings set api_key_encrypted = ?, client_id_encrypted = ?, bound_account = ?, status = 'BOUND', updated_at = now() " +
            "where id = (select id from ima_skill_bindings order by created_at limit 1)",
        "encrypted:" + apiKey,
        "encrypted:" + clientId,
        boundAccount == null || boundAccount.isBlank() ? "ima account" : boundAccount
    );
    return status();
  }

  public Map<String, Object> syncSubscriptions() {
    Map<String, Object> response = searchKnowledgeBases();
    List<Map<String, Object>> items = extractList(response, "info_list");
    Map<String, Boolean> addable = loadAddableKnowledgeBases();
    for (Map<String, Object> item : items) {
      String remoteId = firstText(item, "id", "kb_id", "knowledge_base_id");
      String name = firstText(item, "name", "kb_name", "knowledge_base_name");
      if (remoteId.isBlank() || name.isBlank()) {
        continue;
      }
      String coverUrl = firstText(item, "cover_url", "coverUrl");
      boolean canAdd = addable.getOrDefault(remoteId, false);
      String description = firstText(item, "description", "desc");
      String baseType = firstText(item, "base_type", "baseType");
      String roleType = firstText(item, "role_type", "roleType");
      String scopePrompt = String.join("，", Arrays.asList(
          "ima账号订阅库",
          baseType.isBlank() ? "知识库" : baseType,
          roleType.isBlank() ? "可检索" : roleType,
          description.isBlank() ? (canAdd ? "可检索，可添加内容" : "可检索") : description
      ));
      jdbcTemplate.update(
          "insert into ima_knowledge_bases(knowledge_base_id, name, cover_url, can_add, source_type, raw_json, synced_at, updated_at) " +
              "values (?, ?, ?, ?, 'SUBSCRIBED', ?::jsonb, now(), now()) " +
              "on conflict (knowledge_base_id) do update set name = excluded.name, cover_url = excluded.cover_url, " +
              "can_add = excluded.can_add, raw_json = excluded.raw_json, synced_at = now(), updated_at = now()",
          remoteId,
          name,
          coverUrl,
          canAdd,
          toJson(item)
      );
      jdbcTemplate.update(
          "insert into knowledge_scopes(name, type, owner, scope_prompt, enabled, external_id, source, cover_url, last_verified_at) " +
              "values (?, 'ima知识库', 'ima账号', ?, true, ?, 'IMA_OPENAPI', ?, now()) " +
              "on conflict (external_id) do update set name = excluded.name, scope_prompt = excluded.scope_prompt, " +
              "enabled = true, source = 'IMA_OPENAPI', cover_url = excluded.cover_url, last_verified_at = now(), updated_at = now()",
          name,
          scopePrompt,
          remoteId,
          coverUrl
      );
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("success", true);
    result.put("syncedCount", items.size());
    result.put("subscriptions", subscriptions());
    result.put("raw", response);
    return result;
  }

  public Map<String, Object> testSearch(String query) {
    Map<String, Object> openApiResult = searchKnowledgeBaseByQuery(query == null ? "" : query);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("query", query);
    payload.put("provider", "ima_skill");
    payload.put("status", "OPENAPI_OK");
    payload.put("time", OffsetDateTime.now());
    payload.put("summary", "已通过 ima OpenAPI 调用 search_knowledge_base。");
    payload.put("apiPath", "openapi/wiki/v1/search_knowledge_base");
    payload.put("raw", openApiResult);
    payload.put("resultCount", extractList(openApiResult, "info_list").size());
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
        || (decrypt((String) binding.get("api_key_encrypted")) != null && decrypt((String) binding.get("client_id_encrypted")) != null)
        || (properties.getSkillKey() != null && !properties.getSkillKey().isBlank());
  }

  private Map<String, Object> safeBinding(Map<String, Object> binding) {
    Map<String, Object> safe = new LinkedHashMap<>(binding);
    String key = decrypt((String) binding.get("api_key_encrypted"));
    String clientId = decrypt((String) binding.get("client_id_encrypted"));
    safe.remove("api_key_encrypted");
    safe.remove("client_id_encrypted");
    safe.put("apiKeyConfigured", key != null && !key.isBlank());
    safe.put("apiKeyMasked", mask(key));
    safe.put("clientIdConfigured", clientId != null && !clientId.isBlank());
    safe.put("clientIdMasked", mask(clientId));
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
    return Arrays.asList("searchKnowledgeBase", "searchKnowledge", "getKnowledgeList", "readNote", "returnCitations", "importUrls");
  }

  private Map<String, Object> syncSummary() {
    Map<String, Object> summary = new LinkedHashMap<>();
    Map<String, Object> row = jdbcTemplate.queryForMap(
        "select count(*) as count, max(synced_at) as last_synced_at from ima_knowledge_bases"
    );
    summary.put("count", row.get("count"));
    summary.put("lastSyncedAt", row.get("last_synced_at"));
    return summary;
  }

  private Map<String, Object> searchKnowledgeBases() {
    return searchKnowledgeBaseByQuery("");
  }

  private Map<String, Object> searchKnowledgeBaseByQuery(String query) {
    return imaOpenApi("openapi/wiki/v1/search_knowledge_base", Map.of(
        "query", query == null ? "" : query,
        "cursor", "",
        "limit", 20
    ));
  }

  private Map<String, Boolean> loadAddableKnowledgeBases() {
    Map<String, Boolean> addable = new HashMap<>();
    try {
      Map<String, Object> response = imaOpenApi("openapi/wiki/v1/get_addable_knowledge_base_list", Map.of(
          "cursor", "",
          "limit", 50
      ));
      for (Map<String, Object> item : extractList(response, "addable_knowledge_base_list")) {
        String id = firstText(item, "id", "kb_id", "knowledge_base_id");
        if (!id.isBlank()) {
          addable.put(id, true);
        }
      }
    } catch (Exception ignored) {
      // Some accounts may only have read permission. Search results are still useful.
    }
    return addable;
  }

  private Map<String, Object> imaOpenApi(String apiPath, Map<String, Object> body) {
    Map<String, Object> binding = jdbcTemplate.queryForMap(
        "select api_key_encrypted, client_id_encrypted from ima_skill_bindings order by created_at limit 1"
    );
    String apiKey = decrypt((String) binding.get("api_key_encrypted"));
    String clientId = decrypt((String) binding.get("client_id_encrypted"));
    if (apiKey == null || apiKey.isBlank() || clientId == null || clientId.isBlank()) {
      throw new IllegalStateException("ima Client ID 或 API Key 为空，请先在 ima Skill 页面完成绑定。");
    }
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(IMA_OPENAPI_BASE + "/" + apiPath))
          .timeout(java.time.Duration.ofSeconds(30))
          .header("Content-Type", "application/json")
          .header("ima-openapi-clientid", clientId)
          .header("ima-openapi-apikey", apiKey)
          .header("ima-openapi-ctx", "skill_version=" + IMA_SKILL_VERSION)
          .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      Map<String, Object> parsed = objectMapper.readValue(response.body(), Map.class);
      parsed.put("_httpStatus", response.statusCode());
      Object code = parsed.get("code");
      if (response.statusCode() < 200 || response.statusCode() >= 300 || (code instanceof Number && ((Number) code).intValue() != 0)) {
        throw new IllegalStateException("ima OpenAPI 调用失败：" + response.body());
      }
      return parsed;
    } catch (Exception ex) {
      throw new IllegalStateException("ima OpenAPI 调用失败：" + ex.getMessage(), ex);
    }
  }

  private List<Map<String, Object>> extractList(Map<String, Object> response, String field) {
    Object data = response.get("data");
    if (!(data instanceof Map)) {
      return new ArrayList<>();
    }
    Object value = ((Map<?, ?>) data).get(field);
    if (!(value instanceof List)) {
      return new ArrayList<>();
    }
    List<Map<String, Object>> result = new ArrayList<>();
    for (Object item : (List<?>) value) {
      if (item instanceof Map) {
        result.add((Map<String, Object>) item);
      }
    }
    return result;
  }

  private String stringValue(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  private String firstText(Map<String, Object> map, String... keys) {
    for (String key : keys) {
      String value = stringValue(map.get(key));
      if (!value.isBlank()) {
        return value;
      }
    }
    return "";
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      return "{}";
    }
  }
}
