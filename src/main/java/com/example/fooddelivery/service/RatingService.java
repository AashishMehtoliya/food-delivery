package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.RatingRequest;
import com.example.fooddelivery.dto.RatingResponse;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.entity.Rating;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.exception.ResourceConflictException;
import com.example.fooddelivery.exception.ResourceNotFoundException;
import com.example.fooddelivery.exception.UnauthorizedActionException;
import com.example.fooddelivery.repository.OrderRepository;
import com.example.fooddelivery.repository.RatingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingService {

	private final RatingRepository ratingRepository;
	private final OrderRepository orderRepository;

	public RatingService(RatingRepository ratingRepository, OrderRepository orderRepository) {
		this.ratingRepository = ratingRepository;
		this.orderRepository = orderRepository;
	}

	@Transactional
	public RatingResponse rate(Long orderId, RatingRequest request, Long customerId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
		if (!order.getCustomer().getId().equals(customerId)) {
			throw new UnauthorizedActionException("You do not own this order");
		}
		if (order.getStatus() != OrderStatus.DELIVERED) {
			throw new ResourceConflictException("Order must be DELIVERED before it can be rated");
		}
		if (ratingRepository.existsByOrderId(orderId)) {
			throw new ResourceConflictException("Order " + orderId + " has already been rated");
		}

		Rating rating = new Rating();
		rating.setOrder(order);
		rating.setCustomer(order.getCustomer());
		rating.setRestaurantRating(request.restaurantRating());
		rating.setPartnerRating(request.partnerRating());
		rating.setComment(request.comment());
		return RatingResponse.from(ratingRepository.save(rating));
	}
}
