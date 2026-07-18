package com.epiis.projectcasaketteler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.epiis.projectcasaketteler.entity.EntityAttendance;
import com.epiis.projectcasaketteler.entity.EntityAttendance.AttendanceEventType;
import com.epiis.projectcasaketteler.entity.EntityUser;

import java.util.Optional;
import java.util.Date;
import java.util.List;

public interface RepositoryAttendance extends JpaRepository<EntityAttendance, String> {

	// Último evento de un usuario (para cooldown y detección de anomalías)
	@Query("SELECT a FROM EntityAttendance a WHERE a.parentUser = :entityUser ORDER BY a.eventTimestamp DESC LIMIT 1")
	Optional<EntityAttendance> findTopByParentUserOrderByEventTimestampDesc(@Param("entityUser") EntityUser entityUser);

	// Último intento fallido reciente de un usuario (anti-spam)
	@Query("SELECT a FROM EntityAttendance a WHERE a.parentUser = :entityUser " +
			"AND a.eventType = com.epiis.projectcasaketteler.entity.EntityAttendance$AttendanceEventType.INTENTO_FALLIDO "
			+
			"ORDER BY a.eventTimestamp DESC LIMIT 1")
	Optional<EntityAttendance> findLastFailedAttempt(@Param("entityUser") EntityUser entityUser);

	// Historial de un residente (su propia asistencia)
	@Query("SELECT a FROM EntityAttendance a WHERE a.parentUser = :user " +
			"AND (:fechaInicio IS NULL OR a.eventTimestamp >= :fechaInicio) " +
			"AND (:fechaFin IS NULL OR a.eventTimestamp <= :fechaFin) " +
			"AND (:tipo IS NULL OR a.eventType = :tipo) " +
			"ORDER BY a.eventTimestamp DESC")
	Page<EntityAttendance> findByFilters(
			@Param("user") EntityUser user,
			@Param("fechaInicio") Date fechaInicio,
			@Param("fechaFin") Date fechaFin,
			@Param("tipo") AttendanceEventType tipo,
			Pageable pageable);

	// Filtro admin sobre eventos, acotado por residencia
	@Query("SELECT a FROM EntityAttendance a WHERE " +
			"(:fechaInicio IS NULL OR a.eventTimestamp >= :fechaInicio) " +
			"AND (:fechaFin IS NULL OR a.eventTimestamp <= :fechaFin) " +
			"AND (:tipo IS NULL OR a.eventType = :tipo) " +
			"AND (:idUser IS NULL OR a.parentUser.idUser = :idUser) " +
			"AND (:idResidence IS NULL OR a.parentUser.parentResidence.idResidence = :idResidence) " +
			"ORDER BY a.eventTimestamp DESC")
	Page<EntityAttendance> findByFiltersAdmin(
			@Param("fechaInicio") Date fechaInicio,
			@Param("fechaFin") Date fechaFin,
			@Param("tipo") AttendanceEventType tipo,
			@Param("idUser") String idUser,
			@Param("idResidence") String idResidence,
			Pageable pageable);

	// Solo anomalías (panel de disciplina del admin)
	@Query("SELECT a FROM EntityAttendance a WHERE a.esAnomalia = true " +
			"AND (:fechaInicio IS NULL OR a.eventTimestamp >= :fechaInicio) " +
			"AND (:fechaFin IS NULL OR a.eventTimestamp <= :fechaFin) " +
			"AND (:idResidence IS NULL OR a.parentUser.parentResidence.idResidence = :idResidence) " +
			"ORDER BY a.eventTimestamp DESC")
	Page<EntityAttendance> findAnomalias(
			@Param("fechaInicio") Date fechaInicio,
			@Param("fechaFin") Date fechaFin,
			@Param("idResidence") String idResidence,
			Pageable pageable);

	@Query("SELECT a FROM EntityAttendance a WHERE a.parentUser = :user ORDER BY a.eventTimestamp DESC")
	List<EntityAttendance> findByParentUserOrderByEventTimestampDesc(@Param("user") EntityUser user);
}