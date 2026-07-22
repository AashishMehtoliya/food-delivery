package com.example.fooddelivery.repository;

import com.example.fooddelivery.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, Long> {

	boolean existsByOrderId(Long orderId);
}
