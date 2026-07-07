package com.solutionpilot.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
  private final JdbcTemplate jdbcTemplate;

  public ProjectService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Map<String, Object>> listProjects() {
    return jdbcTemplate.queryForList(
        "select p.id, p.name, p.customer_name, p.industry, p.customer_type, p.status, p.progress, " +
            "p.created_at, p.updated_at, u.name as creator_name, mp.name as model_profile_name " +
            "from projects p left join users u on u.id = p.creator_id left join model_profiles mp on mp.id = p.model_profile_id " +
            "order by p.updated_at desc"
    );
  }

  public Map<String, Object> getProject(UUID id) {
    Map<String, Object> project = jdbcTemplate.queryForMap(
        "select p.*, u.name as creator_name, mp.name as model_profile_name " +
            "from projects p left join users u on u.id = p.creator_id left join model_profiles mp on mp.id = p.model_profile_id where p.id = ?",
        id
    );
    project.put("knowledgeScopes", jdbcTemplate.queryForList(
        "select ks.id, ks.name, ks.type from knowledge_scopes ks join project_knowledge_scopes pks on pks.knowledge_scope_id = ks.id where pks.project_id = ? order by ks.name",
        id
    ));
    project.put("deliverables", jdbcTemplate.queryForList(
        "select deliverable_type, enabled from project_deliverables where project_id = ? order by deliverable_type",
        id
    ));
    project.put("agentRuns", jdbcTemplate.queryForList(
        "select ar.id, s.code as skill_code, s.name as skill_name, s.output_type, ar.status, ar.output_json::text as output_json, ar.error_message, ar.started_at, ar.finished_at " +
            "from agent_runs ar join agent_skills s on s.id = ar.skill_id where ar.project_id = ? order by ar.started_at desc limit 20",
        id
    ));
    project.put("requirementAnalyses", jdbcTemplate.queryForList(
        "select id, content_json, created_at from requirement_analyses where project_id = ? order by created_at desc",
        id
    ));
    project.put("productMatches", jdbcTemplate.queryForList(
        "select id, requirement_text, product_name, module_name, capability, confidence, adopted, citation_id, created_at " +
            "from product_matches where project_id = ? order by created_at desc",
        id
    ));
    project.put("caseMatches", jdbcTemplate.queryForList(
        "select id, case_name, industry, similarity_reason, reference_content, adopted, citation_id, created_at " +
            "from case_matches where project_id = ? order by created_at desc",
        id
    ));
    project.put("architectures", jdbcTemplate.queryForList(
        "select id, content_json, mermaid_text, created_at from architectures where project_id = ? order by created_at desc",
        id
    ));
    project.put("proposalSections", jdbcTemplate.queryForList(
        "select id, title, sort_order, content_markdown, status from proposal_sections where project_id = ? order by sort_order",
        id
    ));
    project.put("pptPages", jdbcTemplate.queryForList(
        "select id, page_type, title, content_json, sort_order from ppt_pages where project_id = ? order by sort_order",
        id
    ));
    return project;
  }

  @Transactional
  public Map<String, Object> createProject(CreateProjectCommand command) {
    UUID creatorId = command.creatorId;
    if (creatorId == null) {
      creatorId = jdbcTemplate.queryForObject("select id from users where email = 'admin@example.com'", UUID.class);
    }
    UUID modelProfileId = command.modelProfileId;
    if (modelProfileId == null) {
      modelProfileId = jdbcTemplate.queryForObject("select id from model_profiles order by created_at limit 1", UUID.class);
    }
    UUID projectId = jdbcTemplate.queryForObject(
        "insert into projects(name, customer_name, industry, customer_type, background, raw_demand, existing_systems, budget, delivery_time, creator_id, model_profile_id, status, progress) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', 10) returning id",
        UUID.class,
        command.name,
        command.customerName,
        command.industry,
        command.customerType,
        command.background,
        command.rawDemand,
        command.existingSystems,
        command.budget,
        command.deliveryTime,
        creatorId,
        modelProfileId
    );
    List<UUID> scopeIds = command.knowledgeScopeIds;
    if (scopeIds == null || scopeIds.isEmpty()) {
      scopeIds = jdbcTemplate.queryForList("select id from knowledge_scopes where enabled = true order by created_at", UUID.class);
    }
    for (UUID scopeId : scopeIds) {
      jdbcTemplate.update(
          "insert into project_knowledge_scopes(project_id, knowledge_scope_id) values (?, ?) on conflict do nothing",
          projectId,
          scopeId
      );
    }
    List<String> deliverables = command.deliverables;
    if (deliverables == null || deliverables.isEmpty()) {
      deliverables = List.of("WORD", "PPT", "ARCHITECTURE");
    }
    for (String deliverable : deliverables) {
      jdbcTemplate.update(
          "insert into project_deliverables(project_id, deliverable_type, enabled) values (?, ?, true)",
          projectId,
          deliverable
      );
    }
    return getProject(projectId);
  }

  @Transactional
  public Map<String, Object> updateProject(UUID id, CreateProjectCommand command) {
    UUID modelProfileId = command.modelProfileId;
    if (modelProfileId == null) {
      modelProfileId = jdbcTemplate.queryForObject("select model_profile_id from projects where id = ?", UUID.class, id);
    }
    jdbcTemplate.update(
        "update projects set name = ?, customer_name = ?, industry = ?, customer_type = ?, background = ?, " +
            "raw_demand = ?, existing_systems = ?, budget = ?, delivery_time = ?, model_profile_id = ?, updated_at = now() where id = ?",
        command.name,
        command.customerName,
        command.industry,
        command.customerType,
        command.background,
        command.rawDemand,
        command.existingSystems,
        command.budget,
        command.deliveryTime,
        modelProfileId,
        id
    );
    jdbcTemplate.update("delete from project_knowledge_scopes where project_id = ?", id);
    List<UUID> scopeIds = command.knowledgeScopeIds;
    if (scopeIds == null || scopeIds.isEmpty()) {
      scopeIds = jdbcTemplate.queryForList("select id from knowledge_scopes where enabled = true order by created_at", UUID.class);
    }
    for (UUID scopeId : scopeIds) {
      jdbcTemplate.update(
          "insert into project_knowledge_scopes(project_id, knowledge_scope_id) values (?, ?) on conflict do nothing",
          id,
          scopeId
      );
    }
    jdbcTemplate.update("delete from project_deliverables where project_id = ?", id);
    List<String> deliverables = command.deliverables;
    if (deliverables == null || deliverables.isEmpty()) {
      deliverables = List.of("WORD", "PPT", "ARCHITECTURE");
    }
    for (String deliverable : deliverables) {
      jdbcTemplate.update(
          "insert into project_deliverables(project_id, deliverable_type, enabled) values (?, ?, true)",
          id,
          deliverable
      );
    }
    return getProject(id);
  }

  @Transactional
  public void deleteProject(UUID id) {
    jdbcTemplate.update("delete from projects where id = ?", id);
  }

  public static class CreateProjectCommand {
    public String name;
    public String customerName;
    public String industry;
    public String customerType;
    public String background;
    public String rawDemand;
    public String existingSystems;
    public String budget;
    public String deliveryTime;
    public UUID creatorId;
    public UUID modelProfileId;
    public List<UUID> knowledgeScopeIds;
    public List<String> deliverables;
  }
}
