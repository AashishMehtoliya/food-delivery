package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.DeliveryPartnerRequest;
import com.example.fooddelivery.dto.DeliveryPartnerResponse;
import com.example.fooddelivery.entity.DeliveryPartner;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.AvailabilityStatus;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.exception.InvalidRoleAssignmentException;
import com.example.fooddelivery.exception.ResourceConflictException;
import com.example.fooddelivery.exception.ResourceNotFoundException;
import com.example.fooddelivery.repository.DeliveryPartnerRepository;
import com.example.fooddelivery.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryPartnerService {

	private final DeliveryPartnerRepository deliveryPartnerRepository;
	private final UserRepository userRepository;

	public DeliveryPartnerService(
			DeliveryPartnerRepository deliveryPartnerRepository, UserRepository userRepository) {
		this.deliveryPartnerRepository = deliveryPartnerRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public DeliveryPartnerResponse create(DeliveryPartnerRequest request) {
		User user = userRepository.findById(request.userId())
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userId()));
		if (user.getRole() != Role.DELIVERY_PARTNER) {
			throw new InvalidRoleAssignmentException("User " + user.getId() + " is not a DELIVERY_PARTNER");
		}
		if (deliveryPartnerRepository.findByUserId(user.getId()).isPresent()) {
			throw new ResourceConflictException("User " + user.getId() + " is already a delivery partner");
		}
		DeliveryPartner partner = new DeliveryPartner();
		partner.setUser(user);
		partner.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
		return DeliveryPartnerResponse.from(deliveryPartnerRepository.save(partner));
	}

	@Transactional(readOnly = true)
	public List<DeliveryPartnerResponse> listAll() {
		return deliveryPartnerRepository.findAll().stream().map(DeliveryPartnerResponse::from).toList();
	}
}
