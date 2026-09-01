package com.handmadeart.ecommerce.dto.customartwork;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request DTO for creating a new custom artwork request.
 *
 * REST API Spec §13 "Submit custom request":
 *   POST /api/v1/custom-requests
 *   Request: CustomArtworkRequestCreateRequest: product type, description,
 *            design/theme, colors, size, budget range, required date,
 *            additional instructions as approved.
 *
 * Fields map 1-to-1 to the approved CustomOrderRequest entity (Database Design §3.13,
 * FR-CUST-02). The customer (user) ID is never accepted from the client — it is
 * resolved exclusively from the JWT via CurrentUserService.
 */
public class CustomArtworkRequestCreateRequest {

    /** Type of artwork requested (e.g., "Oil Painting", "Watercolor"). NOT NULL. */
    @NotBlank(message = "Product type is required")
    @Size(max = 100, message = "Product type must not exceed 100 characters")
    private String productType;

    /** Customer's description of the desired artwork. NOT NULL. */
    @NotBlank(message = "Description is required")
    private String description;

    /** Optional design theme. */
    @Size(max = 200, message = "Design theme must not exceed 200 characters")
    private String designTheme;

    /** Optional preferred colors. */
    @Size(max = 200, message = "Preferred colors must not exceed 200 characters")
    private String preferredColors;

    /** Optional artwork dimensions/size. */
    @Size(max = 100, message = "Dimensions/size must not exceed 100 characters")
    private String dimensionsSize;

    /** Optional customer budget range. */
    @Size(max = 100, message = "Budget range must not exceed 100 characters")
    private String budgetRange;

    /** Optional customer-requested delivery date. */
    private LocalDate requiredDeliveryDate;

    /** Optional additional instructions. */
    private String additionalInstructions;

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDesignTheme() { return designTheme; }
    public void setDesignTheme(String designTheme) { this.designTheme = designTheme; }

    public String getPreferredColors() { return preferredColors; }
    public void setPreferredColors(String preferredColors) { this.preferredColors = preferredColors; }

    public String getDimensionsSize() { return dimensionsSize; }
    public void setDimensionsSize(String dimensionsSize) { this.dimensionsSize = dimensionsSize; }

    public String getBudgetRange() { return budgetRange; }
    public void setBudgetRange(String budgetRange) { this.budgetRange = budgetRange; }

    public LocalDate getRequiredDeliveryDate() { return requiredDeliveryDate; }
    public void setRequiredDeliveryDate(LocalDate requiredDeliveryDate) {
        this.requiredDeliveryDate = requiredDeliveryDate;
    }

    public String getAdditionalInstructions() { return additionalInstructions; }
    public void setAdditionalInstructions(String additionalInstructions) {
        this.additionalInstructions = additionalInstructions;
    }
}
