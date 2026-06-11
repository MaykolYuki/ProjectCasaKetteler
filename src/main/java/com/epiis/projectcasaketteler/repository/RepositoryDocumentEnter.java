package com.epiis.projectcasaketteler.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.epiis.projectcasaketteler.entity.EntityDocumentEnter;
import com.epiis.projectcasaketteler.entity.EntityUser;

public interface RepositoryDocumentEnter extends JpaRepository<EntityDocumentEnter, String> {

    @Query("SELECT d FROM EntityDocumentEnter d WHERE d.parentUser = :user ORDER BY d.created_at DESC")
    List<EntityDocumentEnter> findByParentUserOrderByCreated_atDesc(@Param("user") EntityUser user);
}