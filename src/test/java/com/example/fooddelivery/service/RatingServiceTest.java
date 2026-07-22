package com.example.fooddelivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fooddelivery.dto.RatingRequest;
import com.example.fooddelivery.dto.RatingResponse;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.entity.Rating;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.exception.ResourceConflictException;
import com.example.fooddelivery.exception.UnauthorizedActionException;
import com.example.fooddelivery.repository.OrderRepository;
import com.example.fooddelivery.repository.RatingRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

	@Mock
	private RatingRepository ratingRepository;

	@Mock
	private OrderRepository orderRepository;

	private RatingService service;

	private static final Long CUSTOMER_ID = 3L;
	private Order order;

	@BeforeEach
	void setUp() {
		service = new RatingService(ratingRepository, orderRepository);

		User customer = new User();
		customer.setId(CUSTOMER_ID);

		order = new Order();
		order.setId(100L);
		order.setCustomer(customer);
		order.setStatus(OrderStatus.DELIVERED);
	}

	@Test
	void rate_deliveredUnratedOrder_succeeds() {
		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
		when(ratingRepository.existsByOrderId(100L)).thenReturn(false);
		when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> {
			Rating rating = inv.getArgument(0);
			rating.setId(1L);
			rating.setCreatedAt(java.time.LocalDateTime.now());
			return rating;
		});

		RatingRequest request = new RatingRequest(5, 4, "Great!");
		RatingResponse response = service.rate(100L, request, CUSTOMER_ID);

		assertThat(response.restaurantRating()).isEqualTo(5);
		assertThat(response.partnerRating()).isEqualTo(4);
	}

	@Test
	void rate_notYetDelivered_throwsConflict() {
		order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

		RatingRequest request = new RatingRequest(5, 5, null);

		assertThrows(ResourceConflictException.class, () -> service.rate(100L, request, CUSTOMER_ID));
		verify(ratingRepository, never()).save(any());
	}

	@Test
	void rate_alreadyRated_throwsConflict() {
		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
		when(ratingRepository.existsByOrderId(100L)).thenReturn(true);

		RatingRequest request = new RatingRequest(5, 5, null);

		assertThrows(ResourceConflictException.class, () -> service.rate(100L, request, CUSTOMER_ID));
		verify(ratingRepository, never()).save(any());
	}

	@Test
	void rate_byNonOwner_throwsUnauthorized() {
		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

		RatingRequest request = new RatingRequest(5, 5, null);

		assertThrows(UnauthorizedActionException.class, () -> service.rate(100L, request, 999L));
		verify(ratingRepository, never()).save(any());
	}
}
