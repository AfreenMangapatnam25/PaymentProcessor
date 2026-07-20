package com.paymentprocessor.authorization.repository;

import com.paymentprocessor.authorization.domain.access.RoleAssignment;
import com.paymentprocessor.authorization.domain.enums.RoleScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, UUID> {
    List<RoleAssignment> findByIdentityId(String identityId);
    List<RoleAssignment> findByIdentityIdAndScope(String identityId, RoleScope scope);
    void deleteByIdentityIdAndRoleId(String identityId, UUID roleId);
    List<RoleAssignment> findByRoleId(UUID roleId);
}
