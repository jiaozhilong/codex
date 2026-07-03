package com.solutionpilot.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowService {
  private final JdbcTemplate jdbcTemplate;

  public WorkflowService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional
  public Map<String, Object> runAgent(UUID projectId, String skillCode) {
    UUID adminId = jdbcTemplate.queryForObject("select id from users where email = 'admin@example.com'", UUID.class);
    UUID workflowRunId = jdbcTemplate.queryForObject(
        "insert into workflow_runs(project_id, status, started_by) values (?, 'RUNNING', ?) returning id",
        UUID.class,
        projectId,
        adminId
    );
    UUID skillId = jdbcTemplate.queryForObject("select id from agent_skills where code = ?", UUID.class, skillCode);
    UUID modelProfileId = jdbcTemplate.queryForObject(
        "select coalesce(model_profile_id, (select id from model_profiles order by created_at limit 1)) from projects where id = ?",
        UUID.class,
        projectId
    );
    String outputJson = mockOutput(skillCode);
    UUID agentRunId = jdbcTemplate.queryForObject(
        "insert into agent_runs(workflow_run_id, project_id, skill_id, model_profile_id, status, input_json, output_json, started_at, finished_at) " +
            "values (?, ?, ?, ?, 'COMPLETED', '{}'::jsonb, ?::jsonb, now(), now()) returning id",
        UUID.class,
        workflowRunId,
        projectId,
        skillId,
        modelProfileId,
        outputJson
    );
    persistSkillOutput(projectId, agentRunId, skillCode);
    jdbcTemplate.update("update workflow_runs set status = 'COMPLETED', finished_at = now() where id = ?", workflowRunId);
    jdbcTemplate.update("update projects set status = 'GENERATED', progress = least(progress + 10, 90), updated_at = now() where id = ?", projectId);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("workflowRunId", workflowRunId);
    result.put("agentRunId", agentRunId);
    result.put("skillCode", skillCode);
    result.put("status", "COMPLETED");
    result.put("output", outputJson);
    return result;
  }

  private void persistSkillOutput(UUID projectId, UUID agentRunId, String skillCode) {
    if ("requirement".equals(skillCode)) {
      jdbcTemplate.update(
          "insert into requirement_analyses(project_id, agent_run_id, content_json) values (?, ?, ?::jsonb)",
          projectId,
          agentRunId,
          "{\"businessGoals\":[\"统一空间底座\",\"提升方案生成效率\"],\"explicitRequirements\":[\"二三维展示\",\"专题图层管理\"],\"risks\":[\"知识引用需复核\"]}"
      );
      return;
    }
    if ("product".equals(skillCode)) {
      UUID citationId = jdbcTemplate.queryForObject(
          "insert into citations(project_id, agent_run_id, source_title, source_uri, source_type, snippet, confidence) " +
              "values (?, ?, 'ima 超图官方知识库 / iPortal 产品文档', 'ima://official/iportal', 'IMA', '资源统一注册、检索、授权和共享能力说明。', 0.8600) returning id",
          UUID.class,
          projectId,
          agentRunId
      );
      jdbcTemplate.update(
          "insert into product_matches(project_id, agent_run_id, requirement_text, product_name, module_name, capability, confidence, adopted, citation_id) " +
              "values (?, ?, '专题图层管理', 'SuperMap iPortal', '资源门户', '提供地图、服务、数据资源统一注册、检索、授权和共享。', 0.8600, true, ?)",
          projectId,
          agentRunId,
          citationId
      );
      return;
    }
    if ("case".equals(skillCode)) {
      UUID citationId = jdbcTemplate.queryForObject(
          "insert into citations(project_id, agent_run_id, source_title, source_uri, source_type, snippet, confidence) " +
              "values (?, ?, 'ima 历史方案案例库 / 自然资源一张图案例', 'ima://cases/natural-resource', 'IMA', '自然资源多源数据融合和专题应用建设思路。', 0.8200) returning id",
          UUID.class,
          projectId,
          agentRunId
      );
      jdbcTemplate.update(
          "insert into case_matches(project_id, agent_run_id, case_name, industry, similarity_reason, reference_content, adopted, citation_id) " +
              "values (?, ?, '某省自然资源一张图项目', '自然资源', '同样涉及多源空间数据融合和专题图层管理。', '可引用统一空间底座、资源目录和专题应用建设思路。', true, ?)",
          projectId,
          agentRunId,
          citationId
      );
      return;
    }
    if ("architecture".equals(skillCode)) {
      jdbcTemplate.update(
          "insert into architectures(project_id, agent_run_id, content_json, mermaid_text) values (?, ?, ?::jsonb, ?)",
          projectId,
          agentRunId,
          "{\"summary\":\"采用数据底座、GIS 平台能力、业务应用和运维安全四层架构。\"}",
          "flowchart TD\n  A[客户需求] --> B[Agent 工作流]\n  B --> C[ima 知识库]\n  B --> D[方案输出]"
      );
      return;
    }
    if ("proposal".equals(skillCode)) {
      jdbcTemplate.update(
          "insert into proposal_sections(project_id, title, sort_order, content_markdown, status) values (?, '项目背景', 1, ?, 'DRAFT')",
          projectId,
          "本章节基于客户原始需求和 ima 知识库引用生成，正式版本会保留引用来源。"
      );
      jdbcTemplate.update(
          "insert into proposal_sections(project_id, title, sort_order, content_markdown, status) values (?, '系统功能设计', 2, ?, 'DRAFT')",
          projectId,
          "围绕 GIS 空间底座、资源门户和专题应用展开功能设计。"
      );
      return;
    }
    if ("ppt".equals(skillCode)) {
      jdbcTemplate.update(
          "insert into ppt_pages(project_id, page_type, title, content_json, sort_order) values (?, 'COVER', '方案封面', ?::jsonb, 1)",
          projectId,
          "{\"subtitle\":\"AI 生成方案初稿\"}"
      );
      jdbcTemplate.update(
          "insert into ppt_pages(project_id, page_type, title, content_json, sort_order) values (?, 'ARCHITECTURE', '技术架构', ?::jsonb, 2)",
          projectId,
          "{\"points\":[\"数据底座\",\"GIS 服务\",\"Agent 工作流\",\"导出服务\"]}"
      );
      return;
    }
    if ("qa".equals(skillCode)) {
      jdbcTemplate.update(
          "insert into citations(project_id, agent_run_id, source_title, source_type, snippet, confidence) values (?, ?, 'QA 检查记录', 'SYSTEM', '发现缺少引用或低置信度内容时需人工复核。', 0.7000)",
          projectId,
          agentRunId
      );
    }
  }

  private String mockOutput(String skillCode) {
    if ("product".equals(skillCode)) {
      return "{\"summary\":\"Product Agent mock completed with ima Skill citation placeholders.\",\"citations\":[\"ima: 超图官方知识库 / 示例来源\"]}";
    }
    if ("requirement".equals(skillCode)) {
      return "{\"businessGoals\":[\"统一空间底座\",\"提升方案生成效率\"],\"risks\":[\"知识引用需复核\"]}";
    }
    return "{\"summary\":\"" + skillCode + " Agent mock completed\"}";
  }
}
