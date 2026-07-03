package com.solutionpilot.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final JdbcTemplate jdbcTemplate;

  public AuthService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Map<String, Object> login(String email, String password) {
    Map<String, Object> user = jdbcTemplate.queryForObject(
        "select id, name, email, status from users where email = ?",
        (rs, rowNum) -> {
          Map<String, Object> item = new LinkedHashMap<>();
          item.put("id", rs.getObject("id", UUID.class));
          item.put("name", rs.getString("name"));
          item.put("email", rs.getString("email"));
          item.put("status", rs.getString("status"));
          return item;
        },
        email
    );
    user.put("roles", jdbcTemplate.queryForList(
        "select r.code from roles r join user_roles ur on ur.role_id = r.id join users u on u.id = ur.user_id where u.email = ? order by r.code",
        String.class,
        email
    ));
    user.put("token", "dev-token-" + user.get("id"));
    return user;
  }

  public Map<String, Object> currentUser(String token) {
    String id = token == null ? "" : token.replace("dev-token-", "");
    return jdbcTemplate.queryForObject(
        "select id, name, email, status from users where id = ?::uuid",
        (rs, rowNum) -> {
          Map<String, Object> item = new LinkedHashMap<>();
          item.put("id", rs.getObject("id", UUID.class));
          item.put("name", rs.getString("name"));
          item.put("email", rs.getString("email"));
          item.put("status", rs.getString("status"));
          return item;
        },
        id
    );
  }
}
