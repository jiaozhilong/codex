package com.solutionpilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "solutionpilot.ima")
public class ImaSkillProperties {
  private String skillEndpoint;
  private String skillKey;

  public String getSkillEndpoint() {
    return skillEndpoint;
  }

  public void setSkillEndpoint(String skillEndpoint) {
    this.skillEndpoint = skillEndpoint;
  }

  public String getSkillKey() {
    return skillKey;
  }

  public void setSkillKey(String skillKey) {
    this.skillKey = skillKey;
  }
}
