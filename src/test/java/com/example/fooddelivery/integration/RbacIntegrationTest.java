package com.example.fooddelivery.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class RbacIntegrationTest extends BaseIntegrationTest {

	@Test
	void customerHittingAdminOnlyEndpoint_getsForbidden() {
		User customer = createUser("Customer", "customer@rbac.test", "password1", Role.CUSTOMER);

		TestRestTemplate authed = restTemplate.withBasicAuth("customer@rbac.test", "password1");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		ResponseEntity<String> response =
				authed.postForEntity("/admin/cities", new HttpEntity<>("{\"name\": \"Anywhere\"}", headers), String.class);

		assertThat(response.getStatusCode().value()).isEqualTo(403);
		assertThat(response.getBody()).contains("ACCESS_DENIED");
	}

	@Test
	void unauthenticatedRequest_getsUnauthorizedWithJsonBody() {
		ResponseEntity<String> response = restTemplate.getForEntity("/admin/restaurants", String.class);

		assertThat(response.getStatusCode().value()).isEqualTo(401);
		assertThat(response.getBody()).contains("UNAUTHENTICATED");
	}

	@Test
	void deliveryPartnerHittingRestaurantOwnerOnlyEndpoint_getsForbidden() {
		User partner = createUser("Partner", "partner@rbac.test", "password1", Role.DELIVERY_PARTNER);

		TestRestTemplate authed = restTemplate.withBasicAuth("partner@rbac.test", "password1");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		ResponseEntity<String> response = authed.postForEntity(
				"/restaurants/1/menu-items",
				new HttpEntity<>("{\"name\": \"x\", \"price\": 1.0, \"stockQuantity\": 1}", headers),
				String.class);

		assertThat(response.getStatusCode().value()).isEqualTo(403);
	}
}
