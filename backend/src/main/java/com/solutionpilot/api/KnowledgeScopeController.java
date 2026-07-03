package com.solutionpilot.api;

import com.solutionpilot.domain.ApiResponse;
import com.solutionpilot.service.KnowledgeScopeService;
import com.solutionpilot.service.KnowledgeScopeService.KnowledgeScopeCommand;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/knowledge-scopes")
public class KnowledgeScopeController {
  private final KnowledgeScopeService knowledgeScopeService;

  public KnowledgeScopeController(KnowledgeScopeService knowledgeScopeService) {
    this.knowledgeScopeService = knowledgeScopeService;
  }

  @GetMapping
  public ApiResponse<List<Map<String, Object>>> listScopes() {
    return ApiResponse.ok(knowledgeScopeService.listScopes());
  }

  @PostMapping
  public ApiResponse<Map<String, Object>> createScope(@Valid @RequestBody KnowledgeScopeRequest request) {
    return ApiResponse.ok(knowledgeScopeService.createScope(request.toCommand()));
  }

  @PutMapping("/{id}")
  public ApiResponse<String> updateScope(@PathVariable UUID id, @Valid @RequestBody KnowledgeScopeRequest request) {
    knowledgeScopeService.updateScope(id, request.toCommand());
    return ApiResponse.ok("updated");
  }

  public static class KnowledgeScopeRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String type;
    private String owner;
    @NotBlank
    private String scopePrompt;
    private boolean enabled = true;

    KnowledgeScopeCommand toCommand() {
      KnowledgeScopeCommand command = new KnowledgeScopeCommand();
      command.name = name;
      command.type = type;
      command.owner = owner;
      command.scopePrompt = scopePrompt;
      command.enabled = enabled;
      return command;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getOwner() {
      return owner;
    }

    public void setOwner(String owner) {
      this.owner = owner;
    }

    public String getScopePrompt() {
      return scopePrompt;
    }

    public void setScopePrompt(String scopePrompt) {
      this.scopePrompt = scopePrompt;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
