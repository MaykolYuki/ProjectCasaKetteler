package com.epiis.projectcasaketteler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.epiis.projectcasaketteler.entity.EntityAttendance;
import com.epiis.projectcasaketteler.entity.EntityUser;

import java.util.Optional;
import java.util.Date;
import java.util.List;

public interface RepositoryAttendance extends JpaRepository<EntityAttendance, String> {

	@Query("SELECT a FROM EntityAttendance a WHERE a.parentUser = :entityUser ORDER BY a.created_at DESC LIMIT 1")
	Optional<EntityAttendance> findTopByParentUserOrderByCreated_atDesc(@Param("entityUser") EntityUser entityUser);

	@Query("SELECT a FROM EntityAttendance a WHERE a.parentUser = :user ORDER BY a.created_at DESC")
	List<EntityAttendance> findByParentUserOrderByCreated_atDesc(@Param("user") EntityUser user);

	@Query("SELECT a FROM EntityAttendance a WHERE a.parentUser = :user " +
			"AND (:fechaInicio IS NULL OR a.created_at >= :fechaInicio) " +
			"AND (:fechaFin IS NULL OR a.created_at <= :fechaFin) " +
			"AND (:estado IS NULL OR a.status = :estado) " +
			"ORDER BY a.created_at DESC")
	Page<EntityAttendance> findByFilters(
			@Param("user") EntityUser user,
			@Param("fechaInicio") Date fechaInicio,
			@Param("fechaFin") Date fechaFin,
			@Param("estado") Boolean estado,
			Pageable pageable);

	@Query("SELECT a FROM EntityAttendance a WHERE " +
			"(:fechaInicio IS NULL OR a.created_at >= :fechaInicio) " +
			"AND (:fechaFin IS NULL OR a.created_at <= :fechaFin) " +
			"AND (:estado IS NULL OR a.status = :estado) " +
			"AND (:idUser IS NULL OR a.parentUser.idUser = :idUser) " +
			"ORDER BY a.created_at DESC")
	Page<EntityAttendance> findByFiltersAdmin(
			@Param("fechaInicio") Date fechaInicio,
			@Param("fechaFin") Date fechaFin,
			@Param("estado") Boolean estado,
			@Param("idUser") String idUser,
			Pageable pageable);

	@Query("SELECT COUNT(DISTINCT a.parentUser.idUser) FROM EntityAttendance a " +
			"WHERE a.status = true " +
			"AND a.created_at >= :inicioDia AND a.created_at < :finDia " +
			"AND (:idResidence IS NULL OR a.parentUser.parentResidence.idResidence = :idResidence)")
	long countPresentesHoy(
			@Param("inicioDia") Date inicioDia,
			@Param("finDia") Date finDia,
			@Param("idResidence") String idResidence);

	default Optional<EntityAttendance> findLastAttendanceByUser(EntityUser user) {
		return findTopByParentUserOrderByCreated_atDesc(user);
	}
}