package com.example.fooddelivery.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.fooddelivery.entity.City;
import com.example.fooddelivery.entity.DeliveryAssignment;
import com.example.fooddelivery.entity.DeliveryPartner;
import com.example.fooddelivery.entity.MenuItem;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.AssignmentStatus;
import com.example.fooddelivery.enums.AvailabilityStatus;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.enums.Role;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class DeliveryOrderStatusIntegrationTest extends BaseIntegrationTest {

	private HttpEntity<String> statusBody(String status) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return new HttpEntity<>("{\"status\": \"" + status + "\"}", headers);
	}

	@Test
	void fullDeliveryLifecycle_advancesThroughOutForDeliveryToDeliveredAndFreesPartner() {
		City city = createCity("Delivery City");
		User owner = createUser("Owner", "owner@delivery.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@delivery.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Diner", city, owner);
		MenuItem item = createMenuItem(restaurant, "Item", new BigDecimal("12.00"), 5);

		Order order = new Order();
		order.setRestaurant(restaurant);
		order.setCustomer(customer);
		order.setStatus(OrderStatus.PREPARING);
		order.setTotalAmount(item.getPrice());
		order = orderRepository.save(order);

		User partnerUser = createUser("Partner", "partner@delivery.test", "password1", Role.DELIVERY_PARTNER);
		DeliveryPartner partner = createDeliveryPartner(partnerUser);
		partner.setAvailabilityStatus(AvailabilityStatus.BUSY);
		deliveryPartnerRepository.save(partner);

		DeliveryAssignment assignment = new DeliveryAssignment();
		assignment.setOrder(order);
		assignment.setStatus(AssignmentStatus.ACCEPTED);
		assignment.setPartner(partner);
		assignment.setOfferedAt(LocalDateTime.now());
		assignment.setAcceptedAt(LocalDateTime.now());
		deliveryAssignmentRepository.save(assignment);

		TestRestTemplate authed = restTemplate.withBasicAuth("partner@delivery.test", "password1");

		ResponseEntity<String> outForDelivery = authed.exchange(
				"/orders/" + order.getId() + "/status", HttpMethod.PATCH, statusBody("OUT_FOR_DELIVERY"), String.class);
		assertThat(outForDelivery.getStatusCode().value()).isEqualTo(200);
		assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
				.isEqualTo(OrderStatus.OUT_FOR_DELIVERY);

		ResponseEntity<String> delivered = authed.exchange(
				"/orders/" + order.getId() + "/status", HttpMethod.PATCH, statusBody("DELIVERED"), String.class);
		assertThat(delivered.getStatusCode().value()).isEqualTo(200);
		assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
				.isEqualTo(OrderStatus.DELIVERED);
		assertThat(deliveryPartnerRepository.findById(partner.getId()).orElseThrow().getAvailabilityStatus())
				.isEqualTo(AvailabilityStatus.AVAILABLE);
	}

	@Test
	void updateStatus_byNonAcceptedPartner_isForbidden() {
		City city = createCity("Forbidden Delivery City");
		User owner = createUser("Owner", "owner@fdelivery.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@fdelivery.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Diner", city, owner);
		MenuItem item = createMenuItem(restaurant, "Item", new BigDecimal("12.00"), 5);

		Order order = new Order();
		order.setRestaurant(restaurant);
		order.setCustomer(customer);
		order.setStatus(OrderStatus.PREPARING);
		order.setTotalAmount(item.getPrice());
		order = orderRepository.save(order);

		User acceptedPartnerUser =
				createUser("AcceptedPartner", "accepted@fdelivery.test", "password1", Role.DELIVERY_PARTNER);
		DeliveryPartner acceptedPartner = createDeliveryPartner(acceptedPartnerUser);

		User otherPartnerUser = createUser("OtherPartner", "other@fdelivery.test", "password1", Role.DELIVERY_PARTNER);
		createDeliveryPartner(otherPartnerUser);

		DeliveryAssignment assignment = new DeliveryAssignment();
		assignment.setOrder(order);
		assignment.setStatus(AssignmentStatus.ACCEPTED);
		assignment.setPartner(acceptedPartner);
		assignment.setOfferedAt(LocalDateTime.now());
		assignment.setAcceptedAt(LocalDateTime.now());
		deliveryAssignmentRepository.save(assignment);

		TestRestTemplate authedOther = restTemplate.withBasicAuth("other@fdelivery.test", "password1");
		ResponseEntity<String> response = authedOther.exchange(
				"/orders/" + order.getId() + "/status", HttpMethod.PATCH, statusBody("OUT_FOR_DELIVERY"), String.class);

		assertThat(response.getStatusCode().value()).isEqualTo(403);
	}

	@Test
	void updateStatus_skippingOutForDelivery_isRejected() {
		City city = createCity("Skip City");
		User owner = createUser("Owner", "owner@skip.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@skip.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Diner", city, owner);
		MenuItem item = createMenuItem(restaurant, "Item", new BigDecimal("12.00"), 5);

		Order order = new Order();
		order.setRestaurant(restaurant);
		order.setCustomer(customer);
		order.setStatus(OrderStatus.PREPARING);
		order.setTotalAmount(item.getPrice());
		order = orderRepository.save(order);

		User partnerUser = createUser("Partner", "partner@skip.test", "password1", Role.DELIVERY_PARTNER);
		DeliveryPartner partner = createDeliveryPartner(partnerUser);

		DeliveryAssignment assignment = new DeliveryAssignment();
		assignment.setOrder(order);
		assignment.setStatus(AssignmentStatus.ACCEPTED);
		assignment.setPartner(partner);
		assignment.setOfferedAt(LocalDateTime.now());
		assignment.setAcceptedAt(LocalDateTime.now());
		deliveryAssignmentRepository.save(assignment);

		TestRestTemplate authed = restTemplate.withBasicAuth("partner@skip.test", "password1");
		ResponseEntity<String> response = authed.exchange(
				"/orders/" + order.getId() + "/status", HttpMethod.PATCH, statusBody("DELIVERED"), String.class);

		assertThat(response.getStatusCode().value()).isEqualTo(409);
		assertThat(response.getBody()).contains("INVALID_STATE_TRANSITION");
	}
}
