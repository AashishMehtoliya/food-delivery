package com.example.fooddelivery.dto;

import com.example.fooddelivery.entity.City;

public record CityResponse(Long id, String name) {

	public static CityResponse from(City city) {
		return new CityResponse(city.getId(), city.getName());
	}
}
