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
import org.springframework.http.ResponseEntity;

class DeliveryAssignmentIntegrationTest extends BaseIntegrationTest {

	@Test
	void accept_singlePartnerWinsAndTransitionsOrderToPreparing() {
		City city = createCity("Assign City");
		User owner = createUser("Owner", "owner@assign.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@assign.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Diner", city, owner);
		MenuItem item = createMenuItem(restaurant, "Item", new BigDecimal("15.00"), 5);

		Order order = new Order();
		order.setRestaurant(restaurant);
		order.setCustomer(customer);
		order.setStatus(OrderStatus.ACCEPTED);
		order.setTotalAmount(item.getPrice());
		order = orderRepository.save(order);

		DeliveryAssignment assignment = new DeliveryAssignment();
		assignment.setOrder(order);
		assignment.setStatus(AssignmentStatus.OFFERED);
		assignment.setOfferedAt(LocalDateTime.now());
		deliveryAssignmentRepository.save(assignment);

		User partnerUser = createUser("Partner", "partner@assign.test", "password1", Role.DELIVERY_PARTNER);
		createDeliveryPartner(partnerUser);

		TestRestTemplate authed = restTemplate.withBasicAuth("partner@assign.test", "password1");
		ResponseEntity<String> response =
				authed.postForEntity("/orders/" + order.getId() + "/assignments/accept", null, String.class);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		DeliveryAssignment refreshed = deliveryAssignmentRepository.findByOrderId(order.getId()).orElseThrow();
		assertThat(refreshed.getStatus()).isEqualTo(AssignmentStatus.ACCEPTED);
		assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
				.isEqualTo(OrderStatus.PREPARING);
	}

	@Test
	void accept_secondAttempt_getsAssignmentAlreadyTaken() {
		City city = createCity("Retry City");
		User owner = createUser("Owner", "owner@retry.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@retry.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Diner", city, owner);
		MenuItem item = createMenuItem(restaurant, "Item", new BigDecimal("15.00"), 5);

		Order order = new Order();
		order.setRestaurant(restaurant);
		order.setCustomer(customer);
		order.setStatus(OrderStatus.ACCEPTED);
		order.setTotalAmount(item.getPrice());
		order = orderRepository.save(order);

		DeliveryAssignment assignment = new DeliveryAssignment();
		assignment.setOrder(order);
		assignment.setStatus(AssignmentStatus.OFFERED);
		assignment.setOfferedAt(LocalDateTime.now());
		deliveryAssignmentRepository.save(assignment);

		User firstPartnerUser = createUser("Partner1", "partner1@retry.test", "password1", Role.DELIVERY_PARTNER);
		createDeliveryPartner(firstPartnerUser);
		User secondPartnerUser = createUser("Partner2", "partner2@retry.test", "password1", Role.DELIVERY_PARTNER);
		createDeliveryPartner(secondPartnerUser);

		TestRestTemplate firstAuthed = restTemplate.withBasicAuth("partner1@retry.test", "password1");
		ResponseEntity<String> firstResponse =
				firstAuthed.postForEntity("/orders/" + order.getId() + "/assignments/accept", null, String.class);
		assertThat(firstResponse.getStatusCode().value()).isEqualTo(200);

		TestRestTemplate secondAuthed = restTemplate.withBasicAuth("partner2@retry.test", "password1");
		ResponseEntity<String> secondResponse =
				secondAuthed.postForEntity("/orders/" + order.getId() + "/assignments/accept", null, String.class);
		assertThat(secondResponse.getStatusCode().value()).isEqualTo(409);
		assertThat(secondResponse.getBody()).contains("ASSIGNMENT_ALREADY_TAKEN");
	}

	@Test
	void concurrentAccept_exactlyOnePartnerWins() throws Exception {
		City city = createCity("Race City");
		User owner = createUser("Owner", "owner@race.test", "password1", Role.RESTAURANT_OWNER);
		User customer = createUser("Customer", "customer@race.test", "password1", Role.CUSTOMER);
		Restaurant restaurant = createRestaurant("Diner", city, owner);
		MenuItem item = createMenuItem(restaurant, "Item", new BigDecimal("15.00"), 5);

		Order order = new Order();
		order.setRestaurant(restaurant);
		order.setCustomer(customer);
		order.setStatus(OrderStatus.ACCEPTED);
		order.setTotalAmount(item.getPrice());
		order = orderRepository.save(order);
		Long orderId = order.getId();

		DeliveryAssignment assignment = new DeliveryAssignment();
		assignment.setOrder(order);
		assignment.setStatus(AssignmentStatus.OFFERED);
		assignment.setOfferedAt(LocalDateTime.now());
		deliveryAssignmentRepository.save(assignment);

		int partnerCount = 10;
		List<TestRestTemplate> partnerClients = new ArrayList<>();
		for (int i = 0; i < partnerCount; i++) {
			String email = "racer" + i + "@race.test";
			User partnerUser = createUser("Racer" + i, email, "password1", Role.DELIVERY_PARTNER);
			createDeliveryPartner(partnerUser);
			partnerClients.add(restTemplate.withBasicAuth(email, "password1"));
		}

		ExecutorService pool = Executors.newFixedThreadPool(partnerCount);
		CountDownLatch ready = new CountDownLatch(partnerCount);
		CountDownLatch start = new CountDownLatch(1);
		List<Callable<Integer>> tasks = new ArrayList<>();
		for (TestRestTemplate client : partnerClients) {
			tasks.add(() -> {
				ready.countDown();
				start.await();
				ResponseEntity<String> resp =
						client.postForEntity("/orders/" + orderId + "/assignments/accept", null, String.class);
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

		long successCount = statusCodes.stream().filter(code -> code == 200).count();
		long conflictCount = statusCodes.stream().filter(code -> code == 409).count();

		assertThat(successCount).isEqualTo(1);
		assertThat(conflictCount).isEqualTo(partnerCount - 1);

		DeliveryAssignment finalAssignment = deliveryAssignmentRepository.findByOrderId(orderId).orElseThrow();
		assertThat(finalAssignment.getStatus()).isEqualTo(AssignmentStatus.ACCEPTED);
		assertThat(finalAssignment.getPartner()).isNotNull();

		long busyPartners = deliveryPartnerRepository.findAll().stream()
				.filter(p -> p.getAvailabilityStatus() == AvailabilityStatus.BUSY)
				.count();
		assertThat(busyPartners).isEqualTo(1);
	}
}
