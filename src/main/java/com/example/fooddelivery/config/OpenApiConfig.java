package com.example.fooddelivery.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String BASIC_AUTH = "basicAuth";

	@Bean
	public OpenAPI foodDeliveryOpenApi() {
		return new OpenAPI()
				.info(new Info().title("Food Delivery API").version("v1"))
				.addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH))
				.components(new Components()
						.addSecuritySchemes(
								BASIC_AUTH,
								new SecurityScheme()
										.name(BASIC_AUTH)
										.type(SecurityScheme.Type.HTTP)
										.scheme("basic")));
	}
}
