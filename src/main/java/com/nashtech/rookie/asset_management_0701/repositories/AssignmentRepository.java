package com.nashtech.rookie.asset_management_0701.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.nashtech.rookie.asset_management_0701.entities.Asset;
import com.nashtech.rookie.asset_management_0701.entities.Assignment;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long>, JpaSpecificationExecutor<Assignment> {
    Page<Assignment> findAllByAsset (Asset asset, Pageable pageable);

    boolean existsByAssetId (Long assetId);
    Optional<Assignment> findByIdAndAssignToUsername (Long id, String assignTo);

}
