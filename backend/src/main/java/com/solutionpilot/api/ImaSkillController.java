package com.solutionpilot.api;

import com.solutionpilot.domain.ApiResponse;
import com.solutionpilot.service.ImaSkillService;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ima-skill")
public class ImaSkillController {
  private final ImaSkillService imaSkillService;

  public ImaSkillController(ImaSkillService imaSkillService) {
    this.imaSkillService = imaSkillService;
  }

  @GetMapping
  public ApiResponse<Map<String, Object>> status() {
    return ApiResponse.ok(imaSkillService.status());
  }

  @PostMapping("/test-search")
  public ApiResponse<Map<String, Object>> testSearch(@Valid @RequestBody TestSearchRequest request) {
    return ApiResponse.ok(imaSkillService.testSearch(request.getQuery()));
  }

  @PostMapping("/bind")
  public ApiResponse<Map<String, Object>> bind(@Valid @RequestBody BindRequest request) {
    return ApiResponse.ok(imaSkillService.bind(request.getApiKey(), request.getBoundAccount()));
  }

  public static class TestSearchRequest {
    @NotBlank
    private String query;

    public String getQuery() {
      return query;
    }

    public void setQuery(String query) {
      this.query = query;
    }
  }

  public static class BindRequest {
    @NotBlank
    private String apiKey;
    private String boundAccount;

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public String getBoundAccount() {
      return boundAccount;
    }

    public void setBoundAccount(String boundAccount) {
      this.boundAccount = boundAccount;
    }
  }
}
