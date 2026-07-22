package com.example.fooddelivery.dto;

import com.example.fooddelivery.entity.Rating;
import java.time.LocalDateTime;

public record RatingResponse(
		Long id,
		Long orderId,
		int restaurantRating,
		int partnerRating,
		String comment,
		LocalDateTime createdAt) {

	public static RatingResponse from(Rating rating) {
		return new RatingResponse(
				rating.getId(),
				rating.getOrder().getId(),
				rating.getRestaurantRating(),
				rating.getPartnerRating(),
				rating.getComment(),
				rating.getCreatedAt());
	}
}
