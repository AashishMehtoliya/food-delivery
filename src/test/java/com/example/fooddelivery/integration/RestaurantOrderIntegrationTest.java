package com.example.fooddelivery.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.fooddelivery.entity.City;
import com.example.fooddelivery.entity.DeliveryAssignment;
import com.example.fooddelivery.entity.MenuItem;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.AssignmentStatus;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.enums.Role;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

class RestaurantOrderIntegrationTest extends BaseIntegrationTest {

	private Order placeOrder(Restaurant restaurant, MenuItem item, User customer) {
		Order order = new Order();
		order.setRestaurant(restaurant);
		order.setCustomer(customer);
		order.setStatus(OrderStatus.PLACED);
		order.setTotalAmount(item.getPrice());
		return orderRepository.save(order);
	}

	@Test
	void accept_transitionsOrderAndCreatesOfferedAssignment() {
		City city = createCity("Accept City");
		User owner = createUser("Owner", "owner@accept.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@accept.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Diner", city, owner);
		MenuItem item = createMenuItem(restaurant, "Item", new BigDecimal("25.00"), 5);
		Order order = placeOrder(restaurant, item, customer);

		TestRestTemplate authed = restTemplate.withBasicAuth("owner@accept.test", "password1");
		ResponseEntity<String> response =
				authed.postForEntity("/orders/" + order.getId() + "/accept", null, String.class);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		Order refreshed = orderRepository.findById(order.getId()).orElseThrow();
		assertThat(refreshed.getStatus()).isEqualTo(OrderStatus.ACCEPTED);

		DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderId(order.getId()).orElseThrow();
		assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.OFFERED);
		assertThat(assignment.getPartner()).isNull();
	}

	@Test
	void accept_byNonOwner_isForbiddenAndLeavesOrderUnchanged() {
		City city = createCity("Forbidden City");
		User owner = createUser("Owner", "owner@forbid.test", "password1", Role.RESTAURANT_OWNER);
		User otherOwner = createUser("Other Owner", "other@forbid.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@forbid.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Diner", city, owner);
		MenuItem item = createMenuItem(restaurant, "Item", new BigDecimal("25.00"), 5);
		Order order = placeOrder(restaurant, item, customer);

		TestRestTemplate authed = restTemplate.withBasicAuth("other@forbid.test", "password1");
		ResponseEntity<String> response =
				authed.postForEntity("/orders/" + order.getId() + "/accept", null, String.class);

		assertThat(response.getStatusCode().value()).isEqualTo(403);
		assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PLACED);
		assertThat(deliveryAssignmentRepository.findByOrderId(order.getId())).isEmpty();
	}

	@Test
	void reject_transitionsOrderToRejected() {
		City city = createCity("Reject City");
		User owner = createUser("Owner", "owner@reject.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@reject.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Diner", city, owner);
		MenuItem item = createMenuItem(restaurant, "Item", new BigDecimal("25.00"), 5);
		Order order = placeOrder(restaurant, item, customer);

		TestRestTemplate authed = restTemplate.withBasicAuth("owner@reject.test", "password1");
		ResponseEntity<String> response =
				authed.postForEntity("/orders/" + order.getId() + "/reject", null, String.class);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.REJECTED);
	}

	@Test
	void cancel_onlyAllowedWhilePlaced() {
		City city = createCity("Cancel City");
		User owner = createUser("Owner", "owner@cancel.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@cancel.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Diner", city, owner);
		MenuItem item = createMenuItem(restaurant, "Item", new BigDecimal("25.00"), 5);
		Order order = placeOrder(restaurant, item, customer);

		TestRestTemplate authed = restTemplate.withBasicAuth("customer@cancel.test", "password1");
		ResponseEntity<String> firstCancel =
				authed.postForEntity("/orders/" + order.getId() + "/cancel", null, String.class);
		assertThat(firstCancel.getStatusCode().value()).isEqualTo(200);
		assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
				.isEqualTo(OrderStatus.CANCELLED);

		ResponseEntity<String> secondCancel =
				authed.postForEntity("/orders/" + order.getId() + "/cancel", null, String.class);
		assertThat(secondCancel.getStatusCode().value()).isEqualTo(409);
		assertThat(secondCancel.getBody()).contains("INVALID_STATE_TRANSITION");
	}

	@Test
	void ownerActionQueue_onlyVisibleToOwningRestaurant() {
		City city = createCity("Queue City");
		User owner = createUser("Owner", "owner@queue.test", "password1", Role.RESTAURANT_OWNER);
		User otherOwner = createUser("Other Owner", "other@queue.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@queue.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Diner", city, owner);
		MenuItem item = createMenuItem(restaurant, "Item", new BigDecimal("25.00"), 5);
		placeOrder(restaurant, item, customer);

		TestRestTemplate authedOwner = restTemplate.withBasicAuth("owner@queue.test", "password1");
		ResponseEntity<String> ownerResponse = authedOwner.getForEntity(
				"/restaurants/" + restaurant.getId() + "/orders?status=PLACED", String.class);
		assertThat(ownerResponse.getStatusCode().value()).isEqualTo(200);
		assertThat(ownerResponse.getBody()).contains("PLACED");

		TestRestTemplate authedOther = restTemplate.withBasicAuth("other@queue.test", "password1");
		ResponseEntity<String> otherResponse = authedOther.getForEntity(
				"/restaurants/" + restaurant.getId() + "/orders?status=PLACED", String.class);
		assertThat(otherResponse.getStatusCode().value()).isEqualTo(403);
	}
}
