package com.epiis.projectcasaketteler.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.epiis.projectcasaketteler.entity.EntityDocumentGeneral;
import com.epiis.projectcasaketteler.entity.EntityUser;

public interface RepositoryDocumentGeneral extends JpaRepository<EntityDocumentGeneral, String> {

        @Query("SELECT d FROM EntityDocumentGeneral d WHERE d.parentUser = :user ORDER BY d.created_at DESC")
        List<EntityDocumentGeneral> findByParentUserOrderByCreated_atDesc(@Param("user") EntityUser user);

        @Query("SELECT d FROM EntityDocumentGeneral d WHERE d.parentUser = :user AND d.type = :type ORDER BY d.created_at DESC")
        List<EntityDocumentGeneral> findByParentUserAndTypeOrderByCreated_atDesc(@Param("user") EntityUser user,
                        @Param("type") String type);

        @Query("SELECT d FROM EntityDocumentGeneral d WHERE d.parentUser = :user AND d.downloadable = true ORDER BY d.created_at DESC")
        List<EntityDocumentGeneral> findByParentUserAndDownloadableTrueOrderByCreated_atDesc(
                        @Param("user") EntityUser user);

        @Query("SELECT d FROM EntityDocumentGeneral d WHERE d.parentUser = :user AND d.type = :type AND d.period = :period ORDER BY d.created_at DESC LIMIT 1")
        Optional<EntityDocumentGeneral> findByParentUserAndTypeAndPeriod(
                        @Param("user") EntityUser user,
                        @Param("type") String type,
                        @Param("period") String period);
}