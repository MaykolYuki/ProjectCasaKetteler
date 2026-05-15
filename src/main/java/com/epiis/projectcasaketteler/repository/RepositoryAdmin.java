package com.epiis.projectcasaketteler.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.epiis.projectcasaketteler.entity.EntityAdmin;



public interface RepositoryAdmin extends JpaRepository<EntityAdmin, String>{
	Optional<EntityAdmin> findByEmail(String email);
}
