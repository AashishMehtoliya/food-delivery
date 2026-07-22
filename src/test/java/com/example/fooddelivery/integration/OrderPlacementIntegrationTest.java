package com.example.fooddelivery.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.fooddelivery.entity.City;
import com.example.fooddelivery.entity.MenuItem;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.entity.Payment;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.enums.PaymentStatus;
import com.example.fooddelivery.enums.Role;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class OrderPlacementIntegrationTest extends BaseIntegrationTest {

	@Test
	void fullOrderPlacementFlow_commitsStockOrderAndPaymentTogether() {
		City city = createCity("Metropolis");
		User owner = createUser("Owner", "owner@flow.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@flow.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Flow Diner", city, owner);
		MenuItem item = createMenuItem(restaurant, "Burger", new BigDecimal("50.00"), 10);

		TestRestTemplate authed = restTemplate.withBasicAuth("customer@flow.test", "password1");
		String body = """
				{"restaurantId": %d, "items": [{"menuItemId": %d, "quantity": 3}]}
				""".formatted(restaurant.getId(), item.getId());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		ResponseEntity<String> response = authed.postForEntity("/orders", new HttpEntity<>(body, headers), String.class);

		assertThat(response.getStatusCode().value()).isEqualTo(201);

		MenuItem refreshedItem = menuItemRepository.findById(item.getId()).orElseThrow();
		assertThat(refreshedItem.getStockQuantity()).isEqualTo(7);

		List<Order> orders = orderRepository.findByCustomerId(customer.getId());
		assertThat(orders).hasSize(1);
		Order order = orders.get(0);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
		assertThat(order.getTotalAmount()).isEqualByComparingTo("150.00");

		Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
		assertThat(payment.getAmount()).isEqualByComparingTo("150.00");

		assertThat(orderItemRepository.findByOrderId(order.getId())).hasSize(1);
	}

	@Test
	void insufficientStock_rejectsWithoutMutatingAnything() {
		City city = createCity("Gotham");
		User owner = createUser("Owner", "owner@stock.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@stock.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Stock Diner", city, owner);
		MenuItem item = createMenuItem(restaurant, "Pizza", new BigDecimal("20.00"), 2);

		TestRestTemplate authed = restTemplate.withBasicAuth("customer@stock.test", "password1");
		String body = """
				{"restaurantId": %d, "items": [{"menuItemId": %d, "quantity": 5}]}
				""".formatted(restaurant.getId(), item.getId());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		ResponseEntity<String> response = authed.postForEntity("/orders", new HttpEntity<>(body, headers), String.class);

		assertThat(response.getStatusCode().value()).isEqualTo(409);
		assertThat(response.getBody()).contains("INSUFFICIENT_STOCK");
		assertThat(menuItemRepository.findById(item.getId()).orElseThrow().getStockQuantity()).isEqualTo(2);
		assertThat(orderRepository.findByCustomerId(customer.getId())).isEmpty();
	}

	@Test
	void paymentFailure_rollsBackStockDecrementAndOrder() {
		City city = createCity("Star City");
		User owner = createUser("Owner", "owner@pay.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@pay.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Pay Diner", city, owner);
		// 13.13 is the documented mock-payment failure trigger amount (PaymentService).
		MenuItem item = createMenuItem(restaurant, "Unlucky Item", new BigDecimal("13.13"), 5);

		TestRestTemplate authed = restTemplate.withBasicAuth("customer@pay.test", "password1");
		String body = """
				{"restaurantId": %d, "items": [{"menuItemId": %d, "quantity": 1}]}
				""".formatted(restaurant.getId(), item.getId());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		ResponseEntity<String> response = authed.postForEntity("/orders", new HttpEntity<>(body, headers), String.class);

		assertThat(response.getStatusCode().value()).isEqualTo(402);
		assertThat(response.getBody()).contains("PAYMENT_FAILED");
		assertThat(menuItemRepository.findById(item.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);
		assertThat(orderRepository.findByCustomerId(customer.getId())).isEmpty();
	}

	@Test
	void concurrentOrders_exactlyStockQuantitySucceedAndStockNeverGoesNegative() throws Exception {
		City city = createCity("Central City");
		User owner = createUser("Owner", "owner@conc.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@conc.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Concurrency Diner", city, owner);
		MenuItem item = createMenuItem(restaurant, "Limited Item", new BigDecimal("10.00"), 5);

		TestRestTemplate authed = restTemplate.withBasicAuth("customer@conc.test", "password1");
		String body = """
				{"restaurantId": %d, "items": [{"menuItemId": %d, "quantity": 1}]}
				""".formatted(restaurant.getId(), item.getId());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

		int threadCount = 10;
		ExecutorService pool = Executors.newFixedThreadPool(threadCount);
		CountDownLatch ready = new CountDownLatch(threadCount);
		CountDownLatch start = new CountDownLatch(1);
		List<Callable<Integer>> tasks = new ArrayList<>();
		for (int i = 0; i < threadCount; i++) {
			tasks.add(() -> {
				ready.countDown();
				start.await();
				ResponseEntity<String> resp = authed.postForEntity("/orders", requestEntity, String.class);
				return resp.getStatusCode().value();
			});
		}

		List<Future<Integer>> futures = new ArrayList<>();
		for (Callable<Integer> task : tasks) {
			futures.add(pool.submit(task));
		}
		ready.await();
		start.countDown();

		List<Integer> statusCodes = new ArrayList<>();
		for (Future<Integer> future : futures) {
			statusCodes.add(future.get(30, TimeUnit.SECONDS));
		}
		pool.shutdown();

		long successCount = statusCodes.stream().filter(code -> code == 201).count();
		long conflictCount = statusCodes.stream().filter(code -> code == 409).count();

		assertThat(successCount).isEqualTo(5);
		assertThat(conflictCount).isEqualTo(5);
		assertThat(menuItemRepository.findById(item.getId()).orElseThrow().getStockQuantity()).isZero();
		assertThat(orderRepository.findByCustomerId(customer.getId())).hasSize(5);
	}
}
