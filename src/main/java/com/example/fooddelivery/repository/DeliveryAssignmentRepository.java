package com.example.fooddelivery.repository;

import com.example.fooddelivery.entity.DeliveryAssignment;
import com.example.fooddelivery.enums.AssignmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {

	Optional<DeliveryAssignment> findByOrderId(Long orderId);

	List<DeliveryAssignment> findByStatus(AssignmentStatus status);
}
