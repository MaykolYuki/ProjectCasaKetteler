package com.epiis.projectcasaketteler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.epiis.projectcasaketteler.entity.EntityAttendance;
import com.epiis.projectcasaketteler.entity.EntityUser;
import java.util.Optional;

public interface RepositoryAttendance extends JpaRepository<EntityAttendance, String> {

	@Query("SELECT a FROM EntityAttendance a WHERE a.parentUser = :entityUser ORDER BY a.created_at DESC LIMIT 1")
	Optional<EntityAttendance> findTopByParentUserOrderByCreated_atDesc(@Param("entityUser") EntityUser entityUser);
}