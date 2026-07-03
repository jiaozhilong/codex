package com.solutionpilot.api;

import com.solutionpilot.domain.ApiResponse;
import com.solutionpilot.service.UserService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  public ApiResponse<List<Map<String, Object>>> listUsers() {
    return ApiResponse.ok(userService.listUsers());
  }

  @GetMapping("/roles")
  public ApiResponse<List<Map<String, Object>>> listRoles() {
    return ApiResponse.ok(userService.listRoles());
  }

  @PostMapping
  public ApiResponse<Map<String, Object>> createUser(@Valid @RequestBody CreateUserRequest request) {
    return ApiResponse.ok(userService.createUser(request.getName(), request.getEmail(), request.getRoleCode()));
  }

  @PutMapping("/{id}/role")
  public ApiResponse<String> updateRole(@PathVariable String id, @Valid @RequestBody UpdateRoleRequest request) {
    userService.updateRole(id, request.getRoleCode());
    return ApiResponse.ok("updated");
  }

  public static class CreateUserRequest {
    @NotBlank
    private String name;
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String roleCode = "WORKER";

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getRoleCode() {
      return roleCode;
    }

    public void setRoleCode(String roleCode) {
      this.roleCode = roleCode;
    }
  }

  public static class UpdateRoleRequest {
    @NotBlank
    private String roleCode;

    public String getRoleCode() {
      return roleCode;
    }

    public void setRoleCode(String roleCode) {
      this.roleCode = roleCode;
    }
  }
}
