package com.example.fooddelivery.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.fooddelivery.entity.City;
import com.example.fooddelivery.entity.MenuItem;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.enums.Role;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class RatingIntegrationTest extends BaseIntegrationTest {

	private Order createOrder(Restaurant restaurant, MenuItem item, User customer, OrderStatus status) {
		Order order = new Order();
		order.setRestaurant(restaurant);
		order.setCustomer(customer);
		order.setStatus(status);
		order.setTotalAmount(item.getPrice());
		return orderRepository.save(order);
	}

	private HttpEntity<String> ratingBody(int restaurantRating, int partnerRating) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String body = "{\"restaurantRating\": " + restaurantRating + ", \"partnerRating\": " + partnerRating + "}";
		return new HttpEntity<>(body, headers);
	}

	@Test
	void rate_deliveredOrder_succeedsOnce() {
		City city = createCity("Rating City");
		User owner = createUser("Owner", "owner@rating.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@rating.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Diner", city, owner);
		MenuItem item = createMenuItem(restaurant, "Item", new BigDecimal("18.00"), 5);
		Order order = createOrder(restaurant, item, customer, OrderStatus.DELIVERED);

		TestRestTemplate authed = restTemplate.withBasicAuth("customer@rating.test", "password1");

		ResponseEntity<String> first =
				authed.postForEntity("/orders/" + order.getId() + "/rating", ratingBody(5, 4), String.class);
		assertThat(first.getStatusCode().value()).isEqualTo(201);
		assertThat(ratingRepository.existsByOrderId(order.getId())).isTrue();

		ResponseEntity<String> second =
				authed.postForEntity("/orders/" + order.getId() + "/rating", ratingBody(3, 3), String.class);
		assertThat(second.getStatusCode().value()).isEqualTo(409);
	}

	@Test
	void rate_orderNotYetDelivered_isRejected() {
		City city = createCity("Early Rating City");
		User owner = createUser("Owner", "owner@early.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@early.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Diner", city, owner);
		MenuItem item = createMenuItem(restaurant, "Item", new BigDecimal("18.00"), 5);
		Order order = createOrder(restaurant, item, customer, OrderStatus.OUT_FOR_DELIVERY);

		TestRestTemplate authed = restTemplate.withBasicAuth("customer@early.test", "password1");
		ResponseEntity<String> response =
				authed.postForEntity("/orders/" + order.getId() + "/rating", ratingBody(5, 5), String.class);

		assertThat(response.getStatusCode().value()).isEqualTo(409);
		assertThat(ratingRepository.existsByOrderId(order.getId())).isFalse();
	}

	@Test
	void rate_byNonOwningCustomer_isForbidden() {
		City city = createCity("Wrong Owner City");
		User owner = createUser("Owner", "owner@wrongowner.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@wrongowner.test", "password1", Role.CUSTOMER);
		User otherCustomer = createUser("Other", "other@wrongowner.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Diner", city, owner);
		MenuItem item = createMenuItem(restaurant, "Item", new BigDecimal("18.00"), 5);
		Order order = createOrder(restaurant, item, customer, OrderStatus.DELIVERED);

		TestRestTemplate authedOther = restTemplate.withBasicAuth("other@wrongowner.test", "password1");
		ResponseEntity<String> response =
				authedOther.postForEntity("/orders/" + order.getId() + "/rating", ratingBody(1, 1), String.class);

		assertThat(response.getStatusCode().value()).isEqualTo(403);
	}
}
