package com.example.fooddelivery.dto;

import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.Role;

public record UserResponse(Long id, String name, String email, Role role) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
	}
}
