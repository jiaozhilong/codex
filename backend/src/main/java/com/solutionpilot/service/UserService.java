package com.solutionpilot.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final JdbcTemplate jdbcTemplate;

  public UserService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Map<String, Object>> listUsers() {
    return jdbcTemplate.queryForList(
        "select u.id, u.name, u.email, u.status, coalesce(string_agg(r.code, ','), '') as roles, coalesce(string_agg(r.name, ','), '') as role_names " +
            "from users u left join user_roles ur on ur.user_id = u.id left join roles r on r.id = ur.role_id " +
            "group by u.id order by u.created_at"
    );
  }

  public List<Map<String, Object>> listRoles() {
    return jdbcTemplate.queryForList("select id, code, name from roles order by code");
  }

  public Map<String, Object> createUser(String name, String email, String roleCode) {
    return jdbcTemplate.queryForObject(
        "with new_user as (" +
            "insert into users(name, email, password_hash, status) values (?, ?, '{demo}user', 'ENABLED') returning id, name, email, status" +
            "), role_row as (select id from roles where code = ?), ins as (" +
            "insert into user_roles(user_id, role_id) select new_user.id, role_row.id from new_user, role_row" +
            ") select id, name, email, status from new_user",
        (rs, rowNum) -> {
          Map<String, Object> item = new LinkedHashMap<>();
          item.put("id", rs.getObject("id"));
          item.put("name", rs.getString("name"));
          item.put("email", rs.getString("email"));
          item.put("status", rs.getString("status"));
          return item;
        },
        name,
        email,
        roleCode
    );
  }

  public void updateRole(String userId, String roleCode) {
    jdbcTemplate.update("delete from user_roles where user_id = ?::uuid", userId);
    jdbcTemplate.update(
        "insert into user_roles(user_id, role_id) select ?::uuid, id from roles where code = ?",
        userId,
        roleCode
    );
  }
}
