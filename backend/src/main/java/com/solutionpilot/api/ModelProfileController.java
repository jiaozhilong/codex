package com.solutionpilot.api;

import com.solutionpilot.domain.ApiResponse;
import com.solutionpilot.service.ModelProfileService;
import com.solutionpilot.service.ModelProfileService.CreateModelProfileCommand;
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
@RequestMapping
public class ModelProfileController {
  private final ModelProfileService modelProfileService;

  public ModelProfileController(ModelProfileService modelProfileService) {
    this.modelProfileService = modelProfileService;
  }

  @GetMapping("/model-providers")
  public ApiResponse<List<Map<String, Object>>> listProviders() {
    return ApiResponse.ok(modelProfileService.listProviders());
  }

  @GetMapping("/model-profiles")
  public ApiResponse<List<Map<String, Object>>> listProfiles() {
    return ApiResponse.ok(modelProfileService.listProfiles());
  }

  @PostMapping("/model-profiles")
  public ApiResponse<Map<String, Object>> createProfile(@Valid @RequestBody ModelProfileRequest request) {
    return ApiResponse.ok(modelProfileService.createProfile(request.toCommand()));
  }

  @PutMapping("/model-profiles/{id}")
  public ApiResponse<String> updateProfile(@PathVariable UUID id, @Valid @RequestBody ModelProfileRequest request) {
    modelProfileService.updateProfile(id, request.toCommand());
    return ApiResponse.ok("updated");
  }

  public static class ModelProfileRequest {
    @NotBlank
    private String providerCode;
    @NotBlank
    private String name;
    @NotBlank
    private String modelName;
    @NotBlank
    private String apiBase;
    private String apiKey;
    private String useFor;
    private String status;

    CreateModelProfileCommand toCommand() {
      CreateModelProfileCommand command = new CreateModelProfileCommand();
      command.providerCode = providerCode;
      command.name = name;
      command.modelName = modelName;
      command.apiBase = apiBase;
      command.apiKey = apiKey;
      command.useFor = useFor;
      command.status = status;
      return command;
    }

    public String getProviderCode() {
      return providerCode;
    }

    public void setProviderCode(String providerCode) {
      this.providerCode = providerCode;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getModelName() {
      return modelName;
    }

    public void setModelName(String modelName) {
      this.modelName = modelName;
    }

    public String getApiBase() {
      return apiBase;
    }

    public void setApiBase(String apiBase) {
      this.apiBase = apiBase;
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public String getUseFor() {
      return useFor;
    }

    public void setUseFor(String useFor) {
      this.useFor = useFor;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }
  }
}
