package com.example.fooddelivery.repository;

import com.example.fooddelivery.entity.City;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, Long> {

	Optional<City> findByName(String name);

	boolean existsByName(String name);
}
