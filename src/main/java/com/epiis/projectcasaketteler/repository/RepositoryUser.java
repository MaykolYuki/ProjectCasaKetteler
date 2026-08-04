package com.epiis.projectcasaketteler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.epiis.projectcasaketteler.entity.EntityUser;
import java.util.Optional;

public interface RepositoryUser extends JpaRepository<EntityUser, String> {

	@Query("SELECT COUNT(u) FROM EntityUser u " +
			"WHERE u.active = true " +
			"AND u.parentResidence.idResidence = :idResidence")
	long countActivosByResidencia(@Param("idResidence") String idResidence);

	@Query("SELECT COUNT(u) FROM EntityUser u WHERE u.active = true AND u.presente = true " +
			"AND (:idResidence IS NULL OR u.parentResidence.idResidence = :idResidence)")
	long countPresentesByResidencia(@Param("idResidence") String idResidence);

	Optional<EntityUser> findByEmail(String email);
}
