package com.example.fooddelivery.integration;

import com.example.fooddelivery.entity.City;
import com.example.fooddelivery.entity.MenuItem;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.repository.CityRepository;
import com.example.fooddelivery.repository.DeliveryAssignmentRepository;
import com.example.fooddelivery.repository.DeliveryPartnerRepository;
import com.example.fooddelivery.repository.MenuItemRepository;
import com.example.fooddelivery.repository.OrderItemRepository;
import com.example.fooddelivery.repository.OrderRepository;
import com.example.fooddelivery.repository.PaymentRepository;
import com.example.fooddelivery.repository.RatingRepository;
import com.example.fooddelivery.repository.RestaurantRepository;
import com.example.fooddelivery.repository.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

	@Autowired
	protected TestRestTemplate restTemplate;

	@Autowired
	protected CityRepository cityRepository;

	@Autowired
	protected UserRepository userRepository;

	@Autowired
	protected RestaurantRepository restaurantRepository;

	@Autowired
	protected MenuItemRepository menuItemRepository;

	@Autowired
	protected DeliveryPartnerRepository deliveryPartnerRepository;

	@Autowired
	protected OrderRepository orderRepository;

	@Autowired
	protected OrderItemRepository orderItemRepository;

	@Autowired
	protected PaymentRepository paymentRepository;

	@Autowired
	protected DeliveryAssignmentRepository deliveryAssignmentRepository;

	@Autowired
	protected RatingRepository ratingRepository;

	@Autowired
	protected PasswordEncoder passwordEncoder;

	// The datasource is file-based H2 (Section 2), so data survives across test runs;
	// each test starts from a clean slate to avoid unique-constraint collisions.
	@BeforeEach
	void cleanDatabase() {
		ratingRepository.deleteAll();
		deliveryAssignmentRepository.deleteAll();
		paymentRepository.deleteAll();
		orderItemRepository.deleteAll();
		orderRepository.deleteAll();
		deliveryPartnerRepository.deleteAll();
		menuItemRepository.deleteAll();
		restaurantRepository.deleteAll();
		userRepository.deleteAll();
		cityRepository.deleteAll();
	}

	protected City createCity(String name) {
		City city = new City();
		city.setName(name);
		return cityRepository.save(city);
	}

	protected User createUser(String name, String email, String rawPassword, Role role) {
		User user = new User();
		user.setName(name);
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(rawPassword));
		user.setRole(role);
		return userRepository.save(user);
	}

	protected Restaurant createRestaurant(String name, City city, User owner) {
		Restaurant restaurant = new Restaurant();
		restaurant.setName(name);
		restaurant.setCity(city);
		restaurant.setOwner(owner);
		return restaurantRepository.save(restaurant);
	}

	protected MenuItem createMenuItem(Restaurant restaurant, String name, BigDecimal price, int stockQuantity) {
		MenuItem item = new MenuItem();
		item.setRestaurant(restaurant);
		item.setName(name);
		item.setPrice(price);
		item.setStockQuantity(stockQuantity);
		item.setAvailable(true);
		return menuItemRepository.save(item);
	}
}
