package com.example.fooddelivery.repository;

import com.example.fooddelivery.entity.Restaurant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

	List<Restaurant> findByCityId(Long cityId);
}
