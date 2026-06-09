package com.epiis.projectcasaketteler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final CorsConfigurationSource corsConfigurationSource;
	private final JwtRequestFilter jwtRequestFilter;

	public SecurityConfig(CorsConfigurationSource corsConfigurationSource, JwtRequestFilter jwtRequestFilter) {
		this.corsConfigurationSource = corsConfigurationSource;
		this.jwtRequestFilter = jwtRequestFilter;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						// ============================================
						// ENDPOINTS PÚBLICOS (no requieren autenticación)
						// ============================================
						.requestMatchers(
								"/casaketteler/login",
								"/casaketteler/registeruser",
								"/casaketteler/registeradmin",
								"/casaketteler/registerresidence",
								"/casaketteler/indexresidence",
								"/casaketteler/showresidence/**",
								"/casaketteler/network/**",
								"/casaketteler/verify"

						).permitAll()

						// ============================================
						// ENDPOINTS QUE SOLO SUPER_ADMIN
						// ============================================
						.requestMatchers("/casaketteler/deleteadmin/**").hasAuthority("SUPER_ADMIN")

						// ============================================
						// ENDPOINTS QUE SOLO ADMIN o SUPER_ADMIN
						// ============================================
						.requestMatchers(
								"/casaketteler/indexadmin",
								"/casaketteler/deactivateuser/**", // <-- Para probar
								"/casaketteler/deleteuser/**", // <-- AGREGAR ESTO
								"/casaketteler/updateuser/**", // <-- AGREGAR ESTO
								"/casaketteler/resetpassword/**", // <-- AGREGAR ESTO
								"/casaketteler/registerphoto")
						.hasAnyAuthority("SUPER_ADMIN", "ADMIN")

						// ============================================
						// RESTO requieren autenticación (cualquier rol)
						// ============================================
						.anyRequest().authenticated())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}
}