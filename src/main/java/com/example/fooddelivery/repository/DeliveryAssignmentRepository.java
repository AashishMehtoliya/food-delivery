package com.example.fooddelivery.repository;

import com.example.fooddelivery.entity.DeliveryAssignment;
import com.example.fooddelivery.entity.DeliveryPartner;
import com.example.fooddelivery.enums.AssignmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {

	Optional<DeliveryAssignment> findByOrderId(Long orderId);

	List<DeliveryAssignment> findByStatus(AssignmentStatus status);

	// Section 10: a single atomic conditional UPDATE, not findById -> check -> save(), so the
	// race between concurrent partners accepting the same offer is resolved by the database,
	// not by application code. Returns the number of rows updated: 1 means this call won the
	// race, 0 means someone else already accepted (or the order/assignment doesn't exist).
	@Modifying(clearAutomatically = true)
	@Query("""
			UPDATE DeliveryAssignment a
			SET a.partner = :partner, a.status = :newStatus, a.acceptedAt = :acceptedAt
			WHERE a.order.id = :orderId AND a.status = :expectedStatus
			""")
	int acceptIfOffered(
			@Param("orderId") Long orderId,
			@Param("partner") DeliveryPartner partner,
			@Param("newStatus") AssignmentStatus newStatus,
			@Param("expectedStatus") AssignmentStatus expectedStatus,
			@Param("acceptedAt") LocalDateTime acceptedAt);
}
