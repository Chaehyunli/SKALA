package com.lecture.enrollment.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcludedPermissionInfo {

    @NotBlank
    private String code;

    @JsonAlias("name")
    private String label;

    private String description;

    @NotBlank
    private String reason;
}
