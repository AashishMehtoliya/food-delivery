package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.RegisterRequest;
import com.example.fooddelivery.dto.UserResponse;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.exception.InvalidRoleAssignmentException;
import com.example.fooddelivery.exception.ResourceConflictException;
import com.example.fooddelivery.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public UserResponse register(RegisterRequest request) {
		if (request.role() == Role.ADMIN) {
			throw new InvalidRoleAssignmentException("Cannot self-register as ADMIN");
		}
		if (userRepository.existsByEmail(request.email())) {
			throw new ResourceConflictException("Email already registered: " + request.email());
		}
		User user = new User();
		user.setName(request.name());
		user.setEmail(request.email());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setRole(request.role());
		User saved = userRepository.save(user);
		return UserResponse.from(saved);
	}
}
