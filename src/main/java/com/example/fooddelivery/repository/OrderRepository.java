package com.example.fooddelivery.repository;

import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.enums.OrderStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

	List<Order> findByRestaurantIdAndStatus(Long restaurantId, OrderStatus status);

	List<Order> findByCustomerId(Long customerId);
}
