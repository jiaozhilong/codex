package com.solutionpilot.api;

import com.solutionpilot.domain.ApiResponse;
import com.solutionpilot.service.SkillService;
import com.solutionpilot.service.SkillService.SkillCommand;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
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

  @PutMapping("/{id}")
  public ApiResponse<String> updateSkill(@PathVariable UUID id, @RequestBody SkillRequest request) {
    SkillCommand command = new SkillCommand();
    command.name = request.getName();
    command.description = request.getDescription();
    command.promptTemplate = request.getPromptTemplate();
    command.toolPolicyJson = request.getToolPolicyJson();
    command.enabled = request.isEnabled();
    skillService.updateSkill(id, command);
    return ApiResponse.ok("updated");
  }

  public static class SkillRequest {
    private String name;
    private String description;
    private String promptTemplate;
    private String toolPolicyJson;
    private boolean enabled;

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
