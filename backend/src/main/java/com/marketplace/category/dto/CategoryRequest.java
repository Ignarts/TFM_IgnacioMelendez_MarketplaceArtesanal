package com.marketplace.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Slug is optional: when blank it is derived from the name. */
public record CategoryRequest(
        @NotBlank @Size(max = 60) String name,
        @Size(max = 60) String slug
) {}
