package com.handmadeart.ecommerce.repository;

import com.handmadeart.ecommerce.entity.CustomOrderRequest;
import com.handmadeart.ecommerce.entity.CustomOrderRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;

/**
 * Spring Data JPA repository for {@link CustomOrderRequest}.
 *
 * Approved operations: FR-CUST-01..11, UC-011..015.
 *
 * Index on {@code custom_order_request.user_id} supports 'My Custom Requests'
 * queries (UC-013, ERD §17).
 * Index on {@code custom_order_request.status} supports Admin review queue
 * filtering (UC-012, ERD §17).
 */
public interface CustomOrderRequestRepository extends JpaRepository<CustomOrderRequest, Long> {

    /** Serialize payment attempts before inspecting the authoritative payable state. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM CustomOrderRequest r WHERE r.user.id = :userId AND r.id = :id")
    java.util.Optional<CustomOrderRequest> findOwnedForPayment(@Param("userId") Long userId, @Param("id") Long id);


    /**
     * Find all custom requests for a given customer.
     * 'My Custom Requests' page — paginated (UC-013, FR-CUST-04).
     * Index on custom_order_request.user_id serves this query.
     */
    Page<CustomOrderRequest> findByUserId(Long userId, Pageable pageable);

    /**
     * Find all requests with a given status.
     * Admin review queue filtering (UC-012, FR-CUST-05).
     * Index on custom_order_request.status serves this query.
     */
    List<CustomOrderRequest> findByStatus(CustomOrderRequestStatus status);

    /**
     * Find requests for a customer with a specific status.
     * Supports customer view filtered by lifecycle stage.
     */
    List<CustomOrderRequest> findByUserIdAndStatus(Long userId, CustomOrderRequestStatus status);
}
