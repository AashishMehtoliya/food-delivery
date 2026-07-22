package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.MenuItemPatchRequest;
import com.example.fooddelivery.dto.MenuItemRequest;
import com.example.fooddelivery.dto.MenuItemResponse;
import com.example.fooddelivery.entity.MenuItem;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.exception.ResourceNotFoundException;
import com.example.fooddelivery.exception.UnauthorizedActionException;
import com.example.fooddelivery.repository.MenuItemRepository;
import com.example.fooddelivery.repository.RestaurantRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuItemService {

	private final MenuItemRepository menuItemRepository;
	private final RestaurantRepository restaurantRepository;

	public MenuItemService(MenuItemRepository menuItemRepository, RestaurantRepository restaurantRepository) {
		this.menuItemRepository = menuItemRepository;
		this.restaurantRepository = restaurantRepository;
	}

	@Transactional
	public MenuItemResponse create(Long restaurantId, MenuItemRequest request, Long ownerId) {
		Restaurant restaurant = findOwnedRestaurant(restaurantId, ownerId);
		MenuItem item = new MenuItem();
		item.setRestaurant(restaurant);
		item.setName(request.name());
		item.setPrice(request.price());
		item.setStockQuantity(request.stockQuantity());
		item.setAvailable(true);
		return MenuItemResponse.from(menuItemRepository.save(item));
	}

	@Transactional
	public MenuItemResponse update(Long restaurantId, Long itemId, MenuItemPatchRequest request, Long ownerId) {
		findOwnedRestaurant(restaurantId, ownerId);
		MenuItem item = menuItemRepository.findById(itemId)
				.orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + itemId));
		if (!item.getRestaurant().getId().equals(restaurantId)) {
			throw new ResourceNotFoundException("Menu item not found: " + itemId);
		}
		if (request.price() != null) {
			item.setPrice(request.price());
		}
		if (request.stockQuantity() != null) {
			item.setStockQuantity(request.stockQuantity());
		}
		if (request.available() != null) {
			item.setAvailable(request.available());
		}
		return MenuItemResponse.from(item);
	}

	@Transactional(readOnly = true)
	public List<MenuItemResponse> listAvailableMenu(Long restaurantId) {
		if (!restaurantRepository.existsById(restaurantId)) {
			throw new ResourceNotFoundException("Restaurant not found: " + restaurantId);
		}
		return menuItemRepository.findByRestaurantIdAndAvailableTrue(restaurantId).stream()
				.map(MenuItemResponse::from)
				.toList();
	}

	private Restaurant findOwnedRestaurant(Long restaurantId, Long ownerId) {
		Restaurant restaurant = restaurantRepository.findById(restaurantId)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));
		if (!restaurant.getOwner().getId().equals(ownerId)) {
			throw new UnauthorizedActionException("You do not own this restaurant");
		}
		return restaurant;
	}
}
