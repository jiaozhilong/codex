package com.solutionpilot.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ModelProfileService {
  private final JdbcTemplate jdbcTemplate;

  public ModelProfileService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Map<String, Object>> listProfiles() {
    return jdbcTemplate.queryForList(
        "select mp.id, mp.name, mp.model_name, mp.api_base, mp.use_for, mp.status, " +
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
