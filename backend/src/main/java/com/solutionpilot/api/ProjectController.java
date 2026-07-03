package com.solutionpilot.api;

import com.solutionpilot.domain.ApiResponse;
import com.solutionpilot.service.ProjectService;
import com.solutionpilot.service.ProjectService.CreateProjectCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects")
public class ProjectController {
  private final ProjectService projectService;

  public ProjectController(ProjectService projectService) {
    this.projectService = projectService;
  }

  @GetMapping
  public ApiResponse<List<Map<String, Object>>> listProjects() {
    return ApiResponse.ok(projectService.listProjects());
  }

  @GetMapping("/{id}")
  public ApiResponse<Map<String, Object>> getProject(@PathVariable UUID id) {
    return ApiResponse.ok(projectService.getProject(id));
  }

  @PostMapping
  public ApiResponse<Map<String, Object>> createProject(@Valid @RequestBody ProjectRequest request) {
    return ApiResponse.ok(projectService.createProject(request.toCommand()));
  }

  public static class ProjectRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String customerName;
    private String industry;
    private String customerType;
    private String background;
    private String rawDemand;
    private String existingSystems;
    private String budget;
    private String deliveryTime;
    private UUID creatorId;
    private UUID modelProfileId;
    private List<UUID> knowledgeScopeIds = new ArrayList<>();
    private List<String> deliverables = new ArrayList<>();

    CreateProjectCommand toCommand() {
      CreateProjectCommand command = new CreateProjectCommand();
      command.name = name;
      command.customerName = customerName;
      command.industry = industry;
      command.customerType = customerType;
      command.background = background;
      command.rawDemand = rawDemand;
      command.existingSystems = existingSystems;
      command.budget = budget;
      command.deliveryTime = deliveryTime;
      command.creatorId = creatorId;
      command.modelProfileId = modelProfileId;
      command.knowledgeScopeIds = knowledgeScopeIds == null ? new ArrayList<>() : knowledgeScopeIds;
      command.deliverables = deliverables == null ? new ArrayList<>() : deliverables;
      return command;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getCustomerName() {
      return customerName;
    }

    public void setCustomerName(String customerName) {
      this.customerName = customerName;
    }

    public String getIndustry() {
      return industry;
    }

    public void setIndustry(String industry) {
      this.industry = industry;
    }

    public String getCustomerType() {
      return customerType;
    }

    public void setCustomerType(String customerType) {
      this.customerType = customerType;
    }

    public String getBackground() {
      return background;
    }

    public void setBackground(String background) {
      this.background = background;
    }

    public String getRawDemand() {
      return rawDemand;
    }

    public void setRawDemand(String rawDemand) {
      this.rawDemand = rawDemand;
    }

    public String getExistingSystems() {
      return existingSystems;
    }

    public void setExistingSystems(String existingSystems) {
      this.existingSystems = existingSystems;
    }

    public String getBudget() {
      return budget;
    }

    public void setBudget(String budget) {
      this.budget = budget;
    }

    public String getDeliveryTime() {
      return deliveryTime;
    }

    public void setDeliveryTime(String deliveryTime) {
      this.deliveryTime = deliveryTime;
    }

    public UUID getCreatorId() {
      return creatorId;
    }

    public void setCreatorId(UUID creatorId) {
      this.creatorId = creatorId;
    }

    public UUID getModelProfileId() {
      return modelProfileId;
    }

    public void setModelProfileId(UUID modelProfileId) {
      this.modelProfileId = modelProfileId;
    }

    public List<UUID> getKnowledgeScopeIds() {
      return knowledgeScopeIds;
    }

    public void setKnowledgeScopeIds(List<UUID> knowledgeScopeIds) {
      this.knowledgeScopeIds = knowledgeScopeIds;
    }

    public List<String> getDeliverables() {
      return deliverables;
    }

    public void setDeliverables(List<String> deliverables) {
      this.deliverables = deliverables;
    }
  }
}
