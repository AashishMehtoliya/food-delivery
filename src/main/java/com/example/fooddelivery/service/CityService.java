package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.CityRequest;
import com.example.fooddelivery.dto.CityResponse;
import com.example.fooddelivery.entity.City;
import com.example.fooddelivery.exception.ResourceConflictException;
import com.example.fooddelivery.repository.CityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CityService {

	private final CityRepository cityRepository;

	public CityService(CityRepository cityRepository) {
		this.cityRepository = cityRepository;
	}

	@Transactional
	public CityResponse create(CityRequest request) {
		if (cityRepository.existsByName(request.name())) {
			throw new ResourceConflictException("City already exists: " + request.name());
		}
		City city = new City();
		city.setName(request.name());
		return CityResponse.from(cityRepository.save(city));
	}
}
