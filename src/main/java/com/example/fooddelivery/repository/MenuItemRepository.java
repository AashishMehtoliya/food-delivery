package com.example.fooddelivery.repository;

import com.example.fooddelivery.entity.MenuItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

	List<MenuItem> findByRestaurantId(Long restaurantId);

	List<MenuItem> findByRestaurantIdAndAvailableTrue(Long restaurantId);
}
