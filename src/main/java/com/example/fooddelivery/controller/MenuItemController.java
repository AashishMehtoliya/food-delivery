package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.MenuItemPatchRequest;
import com.example.fooddelivery.dto.MenuItemRequest;
import com.example.fooddelivery.dto.MenuItemResponse;
import com.example.fooddelivery.security.AppUserPrincipal;
import com.example.fooddelivery.service.MenuItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/restaurants/{restaurantId}/menu-items")
@PreAuthorize("hasRole('RESTAURANT_OWNER')")
public class MenuItemController {

	private final MenuItemService menuItemService;

	public MenuItemController(MenuItemService menuItemService) {
		this.menuItemService = menuItemService;
	}

	@PostMapping
	public ResponseEntity<MenuItemResponse> create(
			@PathVariable Long restaurantId,
			@Valid @RequestBody MenuItemRequest request,
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(menuItemService.create(restaurantId, request, principal.getId()));
	}

	@PatchMapping("/{itemId}")
	public MenuItemResponse update(
			@PathVariable Long restaurantId,
			@PathVariable Long itemId,
			@Valid @RequestBody MenuItemPatchRequest request,
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return menuItemService.update(restaurantId, itemId, request, principal.getId());
	}
}
