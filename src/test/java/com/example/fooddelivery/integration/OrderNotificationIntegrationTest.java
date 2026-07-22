package com.example.fooddelivery.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.fooddelivery.entity.City;
import com.example.fooddelivery.entity.MenuItem;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.event.NotificationRecord;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class OrderNotificationIntegrationTest extends BaseIntegrationTest {

	private List<NotificationRecord> awaitNotifications(Long orderId) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 5000;
		while (System.currentTimeMillis() < deadline) {
			List<NotificationRecord> found = notificationRegistry.findByOrderId(orderId);
			if (!found.isEmpty()) {
				return found;
			}
			Thread.sleep(50);
		}
		return notificationRegistry.findByOrderId(orderId);
	}

	@Test
	void placingAnOrder_notifiesAsynchronouslyOffTheRequestThread() throws InterruptedException {
		City city = createCity("Notify City");
		User owner = createUser("Owner", "owner@notify.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@notify.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Diner", city, owner);
		MenuItem item = createMenuItem(restaurant, "Item", new BigDecimal("20.00"), 5);

		TestRestTemplate authed = restTemplate.withBasicAuth("customer@notify.test", "password1");
		String body = """
				{"restaurantId": %d, "items": [{"menuItemId": %d, "quantity": 1}]}
				""".formatted(restaurant.getId(), item.getId());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		String requestThreadName = Thread.currentThread().getName();
		ResponseEntity<String> response = authed.postForEntity("/orders", new HttpEntity<>(body, headers), String.class);
		assertThat(response.getStatusCode().value()).isEqualTo(201);

		String orderIdStr = response.getBody().replaceAll(".*\"id\":(\\d+).*", "$1");
		Long orderId = Long.valueOf(orderIdStr);

		List<NotificationRecord> notifications = awaitNotifications(orderId);
		assertThat(notifications).hasSize(1);
		NotificationRecord notification = notifications.get(0);
		assertThat(notification.threadName()).isNotEqualTo(requestThreadName);
		assertThat(notification.threadName()).startsWith("notification-");
	}

	@Test
	void rolledBackOrder_producesNoNotification() throws InterruptedException {
		City city = createCity("No Notify City");
		User owner = createUser("Owner", "owner@nonotify.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@nonotify.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Diner", city, owner);
		// 13.13 is the documented mock-payment failure trigger amount (PaymentService).
		MenuItem item = createMenuItem(restaurant, "Unlucky Item", new BigDecimal("13.13"), 5);

		TestRestTemplate authed = restTemplate.withBasicAuth("customer@nonotify.test", "password1");
		String body = """
				{"restaurantId": %d, "items": [{"menuItemId": %d, "quantity": 1}]}
				""".formatted(restaurant.getId(), item.getId());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		ResponseEntity<String> response = authed.postForEntity("/orders", new HttpEntity<>(body, headers), String.class);
		assertThat(response.getStatusCode().value()).isEqualTo(402);

		// Give any (incorrectly) fired async notification a chance to land before asserting
		// its absence.
		Thread.sleep(500);
		assertThat(notificationRegistry.all()).isEmpty();
	}
}
