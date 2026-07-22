package com.example.fooddelivery.dto;

import com.example.fooddelivery.entity.DeliveryPartner;
import com.example.fooddelivery.enums.AvailabilityStatus;

public record DeliveryPartnerResponse(Long id, Long userId, String userName, AvailabilityStatus availabilityStatus) {

	public static DeliveryPartnerResponse from(DeliveryPartner partner) {
		return new DeliveryPartnerResponse(
				partner.getId(),
				partner.getUser().getId(),
				partner.getUser().getName(),
				partner.getAvailabilityStatus());
	}
}
