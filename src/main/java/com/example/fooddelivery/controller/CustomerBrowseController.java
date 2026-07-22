package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.MenuItemResponse;
import com.example.fooddelivery.dto.RestaurantResponse;
import com.example.fooddelivery.service.MenuItemService;
import com.example.fooddelivery.service.RestaurantService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomerBrowseController {

	private final RestaurantService restaurantService;
	private final MenuItemService menuItemService;

	public CustomerBrowseController(RestaurantService restaurantService, MenuItemService menuItemService) {
		this.restaurantService = restaurantService;
		this.menuItemService = menuItemService;
	}

	@GetMapping("/cities/{cityId}/restaurants")
	public List<RestaurantResponse> restaurantsInCity(@PathVariable Long cityId) {
		return restaurantService.listByCity(cityId);
	}

	@GetMapping("/restaurants/{restaurantId}/menu")
	public List<MenuItemResponse> menu(@PathVariable Long restaurantId) {
		return menuItemService.listAvailableMenu(restaurantId);
	}
}
