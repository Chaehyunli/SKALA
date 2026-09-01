package com.lecture.enrollment.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentInfo {

    @NotBlank
    private String agentCode;

    @Valid
    @NotEmpty
    private List<PermissionInfo> permissions;

    @Valid
    @Builder.Default
    private List<ExcludedPermissionInfo> excludedPermissions = List.of();
}
