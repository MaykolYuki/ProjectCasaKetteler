package com.epiis.projectcasaketteler.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.epiis.projectcasaketteler.entity.EntityUser;
import java.util.Optional;


public interface RepositoryUser extends JpaRepository<EntityUser, String>{
	Optional<EntityUser> findByEmail(String email);
}
