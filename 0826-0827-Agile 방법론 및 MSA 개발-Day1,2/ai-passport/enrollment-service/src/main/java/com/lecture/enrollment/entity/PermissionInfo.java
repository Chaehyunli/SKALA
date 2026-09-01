package com.lecture.enrollment.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionInfo {

    @NotBlank
    private String code;

    @NotBlank
    @JsonAlias("name")
    private String label;

    private String description;

    @NotBlank
    private String reason;
}
