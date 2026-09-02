package com.handmadeart.ecommerce.dto.order;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for POST /api/v1/checkout/validate (pre-order validation).
 *
 * This endpoint is NON-MUTATING — no order, inventory change, or cart change occurs.
 * The response reflects the current state of the cart and prices at validation time.
 *
 * REST API Spec §9 "Pre-order validation":
 *   Returns: validation result, item summaries, server-computed totals.
 *
 * DEC-007 DEFERRED: totalAmount = subtotalAmount (no tax/delivery charge).
 */
public class CheckoutValidationResponse {

    private boolean valid;

    private List<ValidationItemSummary> items;

    private BigDecimal subtotalAmount;
    private BigDecimal totalAmount;

    public CheckoutValidationResponse() {
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public List<ValidationItemSummary> getItems() { return items; }
    public void setItems(List<ValidationItemSummary> items) { this.items = items; }

    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(BigDecimal subtotalAmount) { this.subtotalAmount = subtotalAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    // -------------------------------------------------------------------------
    // Nested summary per validated cart item
    // -------------------------------------------------------------------------

    public static class ValidationItemSummary {
        private Long productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;

        public ValidationItemSummary() {
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

        public BigDecimal getLineTotal() { return lineTotal; }
        public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
    }
}
