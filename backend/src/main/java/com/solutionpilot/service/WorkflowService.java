package com.solutionpilot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowService {
  private final JdbcTemplate jdbcTemplate;
  private final ModelProfileService modelProfileService;
  private final ObjectMapper objectMapper;

  public WorkflowService(JdbcTemplate jdbcTemplate, ModelProfileService modelProfileService, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.modelProfileService = modelProfileService;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public Map<String, Object> runAgent(UUID projectId, String skillCode, UUID selectedModelProfileId) {
    UUID adminId = jdbcTemplate.queryForObject("select id from users where email = 'admin@example.com'", UUID.class);
    UUID workflowRunId = jdbcTemplate.queryForObject(
        "insert into workflow_runs(project_id, status, started_by) values (?, 'RUNNING', ?) returning id",
        UUID.class,
        projectId,
        adminId
    );
    Map<String, Object> skill = jdbcTemplate.queryForMap(
        "select id, code, name, output_type, prompt_template, tool_policy_json, enabled from agent_skills where code = ?",
        skillCode
    );
    if (!Boolean.TRUE.equals(skill.get("enabled"))) {
      throw new IllegalStateException("Agent skill is disabled: " + skillCode);
    }
    UUID modelProfileId = selectedModelProfileId == null
        ? jdbcTemplate.queryForObject(
            "select coalesce(model_profile_id, (select id from model_profiles order by created_at limit 1)) from projects where id = ?",
            UUID.class,
            projectId
        )
        : selectedModelProfileId;
    jdbcTemplate.update("update projects set model_profile_id = ?, updated_at = now() where id = ?", modelProfileId, projectId);

    Map<String, Object> project = jdbcTemplate.queryForMap("select * from projects where id = ?", projectId);
    String prompt = buildPrompt(project, skill);
    Map<String, Object> input = new LinkedHashMap<>();
    input.put("project", project);
    input.put("skillCode", skillCode);
    input.put("prompt", prompt);

    String answer;
    String errorMessage = null;
    try {
      Map<String, Object> chat = modelProfileService.chat(modelProfileId, prompt);
      answer = String.valueOf(chat.getOrDefault("answer", ""));
      if (answer.isBlank()) {
        answer = fallbackAnswer(project, skillCode);
      }
    } catch (Exception ex) {
      errorMessage = ex.getMessage();
      answer = fallbackAnswer(project, skillCode) + "\n\n> 模型调用失败，已使用本地兜底生成：" + errorMessage;
    }

    Map<String, Object> output = new LinkedHashMap<>();
    output.put("skillCode", skillCode);
    output.put("answer", answer);
    output.put("modelProfileId", modelProfileId);
    output.put("fallback", errorMessage != null);

    UUID agentRunId = jdbcTemplate.queryForObject(
        "insert into agent_runs(workflow_run_id, project_id, skill_id, model_profile_id, status, input_json, output_json, error_message, started_at, finished_at) " +
            "values (?, ?, ?, ?, 'COMPLETED', ?::jsonb, ?::jsonb, ?, now(), now()) returning id",
        UUID.class,
        workflowRunId,
        projectId,
        skill.get("id"),
        modelProfileId,
        json(input),
        json(output),
        errorMessage
    );
    persistSkillOutput(project, agentRunId, skill, answer);
    jdbcTemplate.update("update workflow_runs set status = 'COMPLETED', finished_at = now() where id = ?", workflowRunId);
    jdbcTemplate.update("update projects set status = 'GENERATED', progress = least(progress + 12, 100), updated_at = now() where id = ?", projectId);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("workflowRunId", workflowRunId);
    result.put("agentRunId", agentRunId);
    result.put("skillCode", skillCode);
    result.put("status", "COMPLETED");
    result.put("modelProfileId", modelProfileId);
    result.put("fallback", errorMessage != null);
    result.put("answer", answer);
    return result;
  }

  private String buildPrompt(Map<String, Object> project, Map<String, Object> skill) {
    return "你是 SolutionPilot 的 " + skill.get("name") + "。\n"
        + "请基于以下项目上下文输出可直接进入方案资产的数据，内容必须具体、面向客户、不要泛泛而谈。\n\n"
        + "项目名称：" + value(project, "name") + "\n"
        + "客户名称：" + value(project, "customer_name") + "\n"
        + "行业：" + value(project, "industry") + "\n"
        + "客户类型：" + value(project, "customer_type") + "\n"
        + "项目背景：" + value(project, "background") + "\n"
        + "原始需求：" + value(project, "raw_demand") + "\n"
        + "已有系统：" + value(project, "existing_systems") + "\n"
        + "预算：" + value(project, "budget") + "\n"
        + "交付时间：" + value(project, "delivery_time") + "\n\n"
        + "Agent 配置 Prompt：\n" + skill.get("prompt_template") + "\n\n"
        + "输出要求：使用中文，给出条目化结果，保留能进入方案正文的描述。";
  }

  private void persistSkillOutput(Map<String, Object> project, UUID agentRunId, Map<String, Object> skill, String answer) {
    UUID projectId = (UUID) project.get("id");
    String skillCode = value(skill, "code");
    String outputType = value(skill, "output_type");
    if ("requirement".equals(skillCode) || "REQUIREMENT".equals(outputType)) {
      jdbcTemplate.update(
          "insert into requirement_analyses(project_id, agent_run_id, content_json) values (?, ?, ?::jsonb)",
          projectId,
          agentRunId,
          json(Map.of(
              "businessGoals", List.of("统一数据底座", "提升业务协同", "支撑领导决策"),
              "explicitRequirements", List.of(value(project, "raw_demand")),
              "risks", List.of("数据治理边界需确认", "系统集成接口需核实", "知识库依据需复核"),
              "analysisText", answer
          ))
      );
      return;
    }
    if ("product".equals(skillCode) || "PRODUCT".equals(outputType)) {
      UUID citationId = insertCitation(projectId, agentRunId, "ima 订阅库 / 产品能力检索", "ima://subscriptions/products", answer, 0.8200);
      jdbcTemplate.update(
          "insert into product_matches(project_id, agent_run_id, requirement_text, product_name, module_name, capability, confidence, adopted, citation_id) " +
              "values (?, ?, ?, 'SuperMap GIS 平台能力', 'iPortal / iServer / iDesktop', ?, 0.8200, true, ?)",
          projectId,
          agentRunId,
          value(project, "raw_demand"),
          trim(answer, 900),
          citationId
      );
      return;
    }
    if ("case".equals(skillCode) || "CASE".equals(outputType)) {
      UUID citationId = insertCitation(projectId, agentRunId, "ima 订阅库 / 历史方案案例", "ima://subscriptions/cases", answer, 0.7800);
      jdbcTemplate.update(
          "insert into case_matches(project_id, agent_run_id, case_name, industry, similarity_reason, reference_content, adopted, citation_id) " +
              "values (?, ?, ?, ?, ?, ?, true, ?)",
          projectId,
          agentRunId,
          value(project, "industry") + "类似项目案例",
          value(project, "industry"),
          "与本项目在数据整合、平台建设和业务协同方面具有相似性。",
          trim(answer, 900),
          citationId
      );
      return;
    }
    if ("architecture".equals(skillCode) || "ARCHITECTURE".equals(outputType)) {
      jdbcTemplate.update(
          "insert into architectures(project_id, agent_run_id, content_json, mermaid_text) values (?, ?, ?::jsonb, ?)",
          projectId,
          agentRunId,
          json(Map.of("summary", trim(answer, 1200), "layers", List.of("数据层", "平台能力层", "业务应用层", "运维安全层"))),
          "flowchart TD\n  A[客户需求] --> B[数据治理]\n  B --> C[GIS 平台能力]\n  C --> D[业务应用]\n  D --> E[方案交付]\n  C --> F[运维安全]"
      );
      return;
    }
    if ("proposal".equals(skillCode) || "PROPOSAL".equals(outputType)) {
      jdbcTemplate.update("delete from proposal_sections where project_id = ?", projectId);
      jdbcTemplate.update(
          "insert into proposal_sections(project_id, title, sort_order, content_markdown, status) values (?, '项目背景与建设目标', 1, ?, 'DRAFT')",
          projectId,
          "## 项目背景与建设目标\n\n" + trim(answer, 1600)
      );
      jdbcTemplate.update(
          "insert into proposal_sections(project_id, title, sort_order, content_markdown, status) values (?, '总体方案与实施路径', 2, ?, 'DRAFT')",
          projectId,
          "## 总体方案与实施路径\n\n围绕" + value(project, "customer_name") + "的业务诉求，建议采用数据治理、GIS 平台能力、专题应用建设和持续运维四阶段推进。"
      );
      return;
    }
    if ("ppt".equals(skillCode) || "PPT".equals(outputType)) {
      jdbcTemplate.update("delete from ppt_pages where project_id = ?", projectId);
      jdbcTemplate.update(
          "insert into ppt_pages(project_id, page_type, title, content_json, sort_order) values (?, 'COVER', ?, ?::jsonb, 1)",
          projectId,
          value(project, "name"),
          json(Map.of("subtitle", value(project, "customer_name"), "points", List.of("项目背景", "建设目标", "交付路径")))
      );
      jdbcTemplate.update(
          "insert into ppt_pages(project_id, page_type, title, content_json, sort_order) values (?, 'SOLUTION', '总体方案', ?::jsonb, 2)",
          projectId,
          json(Map.of("points", List.of(trim(answer, 300), "统一空间底座", "多源数据治理", "专题应用支撑")))
      );
      return;
    }
    if ("qa".equals(skillCode) || "QA".equals(outputType)) {
      insertCitation(projectId, agentRunId, "方案质检记录", "system://qa", answer, 0.7000);
      return;
    }
    insertCitation(projectId, agentRunId, value(skill, "name") + " 任务产出", "system://task/" + skillCode, answer, 0.6500);
  }

  private UUID insertCitation(UUID projectId, UUID agentRunId, String title, String uri, String snippet, double confidence) {
    return jdbcTemplate.queryForObject(
        "insert into citations(project_id, agent_run_id, source_title, source_uri, source_type, snippet, confidence) values (?, ?, ?, ?, 'IMA', ?, ?) returning id",
        UUID.class,
        projectId,
        agentRunId,
        title,
        uri,
        trim(snippet, 900),
        confidence
    );
  }

  private String fallbackAnswer(Map<String, Object> project, String skillCode) {
    String customer = value(project, "customer_name");
    String industry = value(project, "industry");
    String demand = value(project, "raw_demand");
    if ("requirement".equals(skillCode)) {
      return customer + "需要围绕" + industry + "业务形成统一数据底座，重点覆盖：" + demand + "。建议补充数据范围、系统接口、权限体系和交付验收标准。";
    }
    if ("product".equals(skillCode)) {
      return "建议匹配 SuperMap GIS 平台能力，包括空间数据管理、二维/三维地图服务、资源门户、专题图层管理、空间分析和服务共享。";
    }
    if ("case".equals(skillCode)) {
      return "可参考同类" + industry + "平台项目，重点借鉴统一空间底座、专题应用集成、数据治理和分阶段实施路径。";
    }
    if ("architecture".equals(skillCode)) {
      return "建议采用数据层、服务层、应用层、安全运维层四层架构，前端承载业务应用，后端统一提供 GIS 服务、知识检索和 Agent 编排能力。";
    }
    if ("proposal".equals(skillCode)) {
      return "方案建议围绕项目背景、建设目标、需求分析、总体架构、功能设计、产品组成、实施计划和价值收益展开。";
    }
    if ("ppt".equals(skillCode)) {
      return "PPT 建议包含封面、现状痛点、建设目标、总体架构、核心能力、实施路径、项目价值和风险保障。";
    }
    return "已完成自定义任务「" + skillCode + "」。建议围绕客户目标、当前问题、可执行动作、依赖条件和交付物验收标准输出任务结果。";
  }

  private String value(Map<String, Object> map, String key) {
    Object value = map.get(key);
    return value == null ? "" : String.valueOf(value);
  }

  private String trim(String text, int max) {
    if (text == null) {
      return "";
    }
    return text.length() <= max ? text : text.substring(0, max);
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("JSON serialization failed", ex);
    }
  }
}
