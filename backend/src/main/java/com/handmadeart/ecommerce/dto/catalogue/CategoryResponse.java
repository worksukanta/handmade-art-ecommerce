package com.handmadeart.ecommerce.dto.catalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.handmadeart.ecommerce.entity.Category;

import java.time.OffsetDateTime;

/**
 * Response DTO for a single active category.
 *
 * Returned by:
 *   GET /api/v1/categories          — list (as CategoryResponse[])
 *   GET /api/v1/categories/{id}     — single item
 *
 * Approved shape (REST API Spec §6, §17): id, name, description, status, created_at.
 * Only ACTIVE categories are returned through public endpoints.
 */
public class CategoryResponse {

    private Long id;
    private String name;
    private String description;
    private String status;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    public CategoryResponse() {
    }

    public static CategoryResponse from(Category category) {
        CategoryResponse dto = new CategoryResponse();
        dto.id = category.getId();
        dto.name = category.getName();
        dto.description = category.getDescription();
        dto.status = category.getStatus().name();
        dto.createdAt = category.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
