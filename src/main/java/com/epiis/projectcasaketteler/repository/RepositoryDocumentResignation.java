package com.epiis.projectcasaketteler.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.epiis.projectcasaketteler.entity.EntityDocumentResignation;
import com.epiis.projectcasaketteler.entity.EntityUser;

public interface RepositoryDocumentResignation extends JpaRepository<EntityDocumentResignation, String> {
    @Query("SELECT d FROM EntityDocumentResignation d WHERE d.parentUser = :user ORDER BY d.created_at DESC")
    List<EntityDocumentResignation> findByParentUserOrderByCreated_atDesc(@Param("user") EntityUser user);

    @Query("SELECT d FROM EntityDocumentResignation d WHERE d.parentUser = :user ORDER BY d.created_at DESC LIMIT 1")
    Optional<EntityDocumentResignation> findTopByParentUserOrderByCreated_atDesc(@Param("user") EntityUser user);
}