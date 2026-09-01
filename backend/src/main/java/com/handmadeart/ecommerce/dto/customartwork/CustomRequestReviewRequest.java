package com.handmadeart.ecommerce.dto.customartwork;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for the Admin "review custom request" operation.
 *
 * REST API Spec §13 "Admin review custom request":
 *   PATCH /api/v1/admin/custom-requests/{id}/review
 *   Request: CustomRequestReviewRequest {decision, notes?}
 *
 * Approved review decisions (mapping to workflow transitions):
 *   "ACCEPT"  → REQUESTED|UNDER_REVIEW → UNDER_REVIEW  (or REQUESTED → UNDER_REVIEW)
 *   "REJECT"  → UNDER_REVIEW → REJECTED
 *
 * The decision string maps to a workflow transition enforced by the service layer.
 * Only valid transitions for the current state are permitted.
 *
 * Notes are optional — stored as reviewNotes on the CustomOrderRequest.
 */
public class CustomRequestReviewRequest {

    /**
     * Review decision. Approved values: "ACCEPT" or "REJECT".
     * "ACCEPT": transitions request to UNDER_REVIEW (from REQUESTED) or keeps UNDER_REVIEW.
     * "REJECT": transitions request from UNDER_REVIEW → REJECTED (terminal).
     */
    @NotBlank(message = "Decision is required")
    @Pattern(regexp = "ACCEPT|REJECT",
             message = "Decision must be ACCEPT or REJECT")
    private String decision;

    /** Optional Admin review notes stored on the request. */
    private String notes;

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
