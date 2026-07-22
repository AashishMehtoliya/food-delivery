package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.CityRequest;
import com.example.fooddelivery.dto.CityResponse;
import com.example.fooddelivery.dto.DeliveryPartnerRequest;
import com.example.fooddelivery.dto.DeliveryPartnerResponse;
import com.example.fooddelivery.dto.RestaurantRequest;
import com.example.fooddelivery.dto.RestaurantResponse;
import com.example.fooddelivery.service.CityService;
import com.example.fooddelivery.service.DeliveryPartnerService;
import com.example.fooddelivery.service.RestaurantService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

	private final CityService cityService;
	private final RestaurantService restaurantService;
	private final DeliveryPartnerService deliveryPartnerService;

	public AdminController(
			CityService cityService,
			RestaurantService restaurantService,
			DeliveryPartnerService deliveryPartnerService) {
		this.cityService = cityService;
		this.restaurantService = restaurantService;
		this.deliveryPartnerService = deliveryPartnerService;
	}

	@PostMapping("/cities")
	public ResponseEntity<CityResponse> createCity(@Valid @RequestBody CityRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(cityService.create(request));
	}

	@PostMapping("/restaurants")
	public ResponseEntity<RestaurantResponse> createRestaurant(@Valid @RequestBody RestaurantRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(restaurantService.create(request));
	}

	@GetMapping("/restaurants")
	public List<RestaurantResponse> listRestaurants() {
		return restaurantService.listAll();
	}

	@PostMapping("/delivery-partners")
	public ResponseEntity<DeliveryPartnerResponse> createDeliveryPartner(
			@Valid @RequestBody DeliveryPartnerRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(deliveryPartnerService.create(request));
	}

	@GetMapping("/delivery-partners")
	public List<DeliveryPartnerResponse> listDeliveryPartners() {
		return deliveryPartnerService.listAll();
	}
}
