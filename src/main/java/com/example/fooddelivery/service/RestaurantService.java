package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.RestaurantRequest;
import com.example.fooddelivery.dto.RestaurantResponse;
import com.example.fooddelivery.entity.City;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.exception.InvalidRoleAssignmentException;
import com.example.fooddelivery.exception.ResourceNotFoundException;
import com.example.fooddelivery.repository.CityRepository;
import com.example.fooddelivery.repository.RestaurantRepository;
import com.example.fooddelivery.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantService {

	private final RestaurantRepository restaurantRepository;
	private final CityRepository cityRepository;
	private final UserRepository userRepository;

	public RestaurantService(
			RestaurantRepository restaurantRepository,
			CityRepository cityRepository,
			UserRepository userRepository) {
		this.restaurantRepository = restaurantRepository;
		this.cityRepository = cityRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public RestaurantResponse create(RestaurantRequest request) {
		City city = cityRepository.findById(request.cityId())
				.orElseThrow(() -> new ResourceNotFoundException("City not found: " + request.cityId()));
		User owner = userRepository.findById(request.ownerId())
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.ownerId()));
		if (owner.getRole() != Role.RESTAURANT_OWNER) {
			throw new InvalidRoleAssignmentException("User " + owner.getId() + " is not a RESTAURANT_OWNER");
		}
		Restaurant restaurant = new Restaurant();
		restaurant.setName(request.name());
		restaurant.setCity(city);
		restaurant.setOwner(owner);
		return RestaurantResponse.from(restaurantRepository.save(restaurant));
	}

	@Transactional(readOnly = true)
	public List<RestaurantResponse> listAll() {
		return restaurantRepository.findAll().stream().map(RestaurantResponse::from).toList();
	}
}
