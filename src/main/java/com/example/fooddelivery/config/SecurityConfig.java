package com.example.fooddelivery.config;

import com.example.fooddelivery.dto.ErrorResponse;
import com.example.fooddelivery.security.AppUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider(
			AppUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
		return (request, response, authException) -> {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			ErrorResponse body = ErrorResponse.of(
					HttpStatus.UNAUTHORIZED.value(), "UNAUTHENTICATED", "Authentication is required");
			objectMapper.writeValue(response.getWriter(), body);
		};
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationEntryPoint authenticationEntryPoint)
			throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/auth/register").permitAll()
						.requestMatchers(HttpMethod.GET, "/cities/*/restaurants", "/restaurants/*/menu")
						.permitAll()
						.requestMatchers(
								"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**")
						.permitAll()
						.anyRequest().authenticated())
				.httpBasic(basic -> basic.authenticationEntryPoint(authenticationEntryPoint))
				.exceptionHandling(handling -> handling.authenticationEntryPoint(authenticationEntryPoint));
		return http.build();
	}
}
