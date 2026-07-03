package com.solutionpilot.api;

import com.solutionpilot.domain.ApiResponse;
import com.solutionpilot.service.SkillService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
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
}
