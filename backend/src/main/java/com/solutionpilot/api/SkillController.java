package com.solutionpilot.api;

import com.solutionpilot.domain.ApiResponse;
import com.solutionpilot.service.SkillService;
import com.solutionpilot.service.SkillService.SkillCommand;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/skills")
public class SkillController {
  private final SkillService skillService;

  public SkillController(SkillService skillService) {
    this.skillService = skillService;
  }

  @GetMapping
  public ApiResponse<List<Map<String, Object>>> listSkills() {
    return ApiResponse.ok(skillService.listSkills());
  }

  @PostMapping
  public ApiResponse<Map<String, Object>> createSkill(@RequestBody SkillRequest request) {
    return ApiResponse.ok(skillService.createSkill(toCommand(request)));
  }

  @PutMapping("/{id}")
  public ApiResponse<String> updateSkill(@PathVariable UUID id, @RequestBody SkillRequest request) {
    skillService.updateSkill(id, toCommand(request));
    return ApiResponse.ok("updated");
  }

  @DeleteMapping("/{id}")
  public ApiResponse<String> disableSkill(@PathVariable UUID id) {
    skillService.disableSkill(id);
    return ApiResponse.ok("disabled");
  }

  private SkillCommand toCommand(SkillRequest request) {
    SkillCommand command = new SkillCommand();
    command.code = request.getCode();
    command.name = request.getName();
    command.description = request.getDescription();
    command.category = request.getCategory();
    command.outputType = request.getOutputType();
    command.sortOrder = request.getSortOrder();
    command.promptTemplate = request.getPromptTemplate();
    command.toolPolicyJson = request.getToolPolicyJson();
    command.enabled = request.isEnabled();
    return command;
  }

  public static class SkillRequest {
    private String code;
    private String name;
    private String description;
    private String category;
    private String outputType;
    private Integer sortOrder;
    private String promptTemplate;
    private String toolPolicyJson;
    private boolean enabled;

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getCategory() {
      return category;
    }

    public void setCategory(String category) {
      this.category = category;
    }

    public String getOutputType() {
      return outputType;
    }

    public void setOutputType(String outputType) {
      this.outputType = outputType;
    }

    public Integer getSortOrder() {
      return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
      this.sortOrder = sortOrder;
    }

    public String getPromptTemplate() {
      return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
      this.promptTemplate = promptTemplate;
    }

    public String getToolPolicyJson() {
      return toolPolicyJson;
    }

    public void setToolPolicyJson(String toolPolicyJson) {
      this.toolPolicyJson = toolPolicyJson;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
