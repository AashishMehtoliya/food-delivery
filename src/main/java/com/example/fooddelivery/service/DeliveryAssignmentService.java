package com.example.fooddelivery.service;

import com.example.fooddelivery.entity.DeliveryAssignment;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.enums.AssignmentStatus;
import com.example.fooddelivery.repository.DeliveryAssignmentRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryAssignmentService {

	private final DeliveryAssignmentRepository deliveryAssignmentRepository;

	public DeliveryAssignmentService(DeliveryAssignmentRepository deliveryAssignmentRepository) {
		this.deliveryAssignmentRepository = deliveryAssignmentRepository;
	}

	@Transactional
	public DeliveryAssignment offer(Order order) {
		DeliveryAssignment assignment = new DeliveryAssignment();
		assignment.setOrder(order);
		assignment.setStatus(AssignmentStatus.OFFERED);
		assignment.setOfferedAt(LocalDateTime.now());
		return deliveryAssignmentRepository.save(assignment);
	}
}
