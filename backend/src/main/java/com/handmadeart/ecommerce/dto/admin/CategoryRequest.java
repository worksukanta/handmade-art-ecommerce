package com.handmadeart.ecommerce.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating a category.
 *
 * Used by:
 *   POST /api/v1/admin/categories
 *   PUT  /api/v1/admin/categories/{id}
 *
 * Approved fields (REST API Spec §6, §17 CategoryRequest/Response):
 *   name        — required, 1–100 chars, must be unique
 *   description — optional free text
 */
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 1, max = 100, message = "Category name must be between 1 and 100 characters")
    private String name;

    private String description;

    public CategoryRequest() {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
