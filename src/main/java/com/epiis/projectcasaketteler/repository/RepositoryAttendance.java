package com.epiis.projectcasaketteler.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.epiis.projectcasaketteler.entity.EntityAttendance;
import com.epiis.projectcasaketteler.entity.EntityUser;

import java.util.Optional;


public interface RepositoryAttendance extends JpaRepository<EntityAttendance, String>{
	Optional<EntityAttendance> findTopByParentUserOrderByCreated_atDesc(EntityUser entityUser);
}
