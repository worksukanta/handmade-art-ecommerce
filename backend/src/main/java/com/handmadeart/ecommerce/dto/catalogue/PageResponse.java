package com.handmadeart.ecommerce.dto.catalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic paginated response envelope.
 *
 * Approved shape (REST API Spec §21):
 *   content, page, size, total_elements, total_pages.
 *
 * Uses zero-based page indexing (consistent with Spring Data Pageable default).
 *
 * @param <T> the element type
 */
public class PageResponse<T> {

    private List<T> content;

    private int page;

    private int size;

    @JsonProperty("total_elements")
    private long totalElements;

    @JsonProperty("total_pages")
    private int totalPages;

    public PageResponse() {
    }

    public static <T> PageResponse<T> from(Page<T> springPage) {
        PageResponse<T> response = new PageResponse<>();
        response.content = springPage.getContent();
        response.page = springPage.getNumber();
        response.size = springPage.getSize();
        response.totalElements = springPage.getTotalElements();
        response.totalPages = springPage.getTotalPages();
        return response;
    }

    public List<T> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
}
