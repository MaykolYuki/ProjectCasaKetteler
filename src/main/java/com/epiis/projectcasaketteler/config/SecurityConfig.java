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
								"/casaketteler/attendance/health")
						.permitAll()

						// ============================================
						// ENDPOINTS QUE SOLO SUPER_ADMIN
						// ============================================
						.requestMatchers(
								"/casaketteler/deleteadmin/**",
								"/casaketteler/registeradmin",
								"/casaketteler/updateadmin/**",
								"/casaketteler/updatepasswordadmin/**",
								"/casaketteler/registerresidence",
								"/casaketteler/updateresidence/**",
								"/casaketteler/deleteresidence/**",
								"/casaketteler/indexresidence",
								"/casaketteler/showresidence/**")
						.hasAuthority("SUPER_ADMIN")

						// ============================================
						// ENDPOINTS QUE SOLO ADMIN o SUPER_ADMIN
						// ============================================
						.requestMatchers(
								"/casaketteler/indexadmin",
								"/casaketteler/showadmin/**",
								"/casaketteler/registeruser",
								"/casaketteler/deactivateuser/**",
								"/casaketteler/deleteuser/**",
								"/casaketteler/updateuser/**",
								"/casaketteler/updatepassworduser/**",
								"/casaketteler/resetpassword/**",
								"/casaketteler/registerphoto",
								"/casaketteler/documents/*", // admin: listar documentos de un residente
																// (documents/{idUser})
								"/casaketteler/documents/*/status", // admin: cambiar estado de un documento
								"/casaketteler/documententer/*", // admin: listar documentos de entrada de un residente
								"/casaketteler/documententer/*/status", // admin: cambiar estado
								"/casaketteler/resignation/*", // admin: ver renuncia de un residente
																// (resignation/{idUser})
								"/casaketteler/resignation/*/status", // admin: cambiar estado de una renuncia
								"/casaketteler/resignation/download/*", // admin: descargar la renuncia subida por el
																		// residente
								"/casaketteler/assignresignation", // admin: asignar formato de renuncia
								"/casaketteler/attendance/filter",
								"/casaketteler/attendance/export",
								"/casaketteler/attendance/kpi")
						.hasAnyAuthority("SUPER_ADMIN", "ADMIN")
						// ============================================
						// RESTO DE LA API: requiere autenticación (cualquier rol)
						// ============================================
						.requestMatchers("/casaketteler/**").authenticated()
						// ============================================
						// Archivos de la interfaz web (Angular) servidos por este mismo
						// backend: son públicos porque son solo el "cascarón" de la app.
						// La protección real está en la API de arriba.
						// ============================================
						.anyRequest().permitAll())
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