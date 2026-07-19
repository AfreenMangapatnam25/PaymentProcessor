package com.paymentprocessor.authorization.service.access;

import com.paymentprocessor.authorization.config.CacheConfig;
import com.paymentprocessor.authorization.domain.access.Permission;
import com.paymentprocessor.authorization.domain.access.Role;
import com.paymentprocessor.authorization.domain.access.RoleAssignment;
import com.paymentprocessor.authorization.repository.RoleAssignmentRepository;
import com.paymentprocessor.authorization.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves effective permissions for identities and roles, with hierarchical role inheritance.
 * Results are cached (short TTL) and evicted explicitly on role/permission/assignment changes to
 * keep authorization latency low.
 */
@Service
@RequiredArgsConstructor
public class PermissionResolver {

    private static final int MAX_INHERITANCE_DEPTH = 10;

    private final RoleRepository roleRepository;
    private final RoleAssignmentRepository assignmentRepository;

    /** Permissions granted to an identity via its active role assignments (cached per identity). */
    @Cacheable(cacheNames = CacheConfig.EFFECTIVE_PERMISSIONS, key = "#identityId")
    @Transactional(readOnly = true)
    public Set<String> assignmentPermissions(String identityId) {
        Instant now = Instant.now();
        List<RoleAssignment> assignments = assignmentRepository.findByIdentityId(identityId);
        Set<String> permissions = new LinkedHashSet<>();
        for (RoleAssignment assignment : assignments) {
            if (assignment.isActive(now)) {
                collect(assignment.getRole(), permissions, 0);
            }
        }
        return permissions;
    }

    /** Permissions carried by a single role, including inherited ones (cached per role). */
    @Cacheable(cacheNames = CacheConfig.ROLE_PERMISSIONS, key = "#roleName")
    @Transactional(readOnly = true)
    public Set<String> permissionsForRole(String roleName) {
        Set<String> permissions = new LinkedHashSet<>();
        roleRepository.findByName(roleName).ifPresent(role -> collect(role, permissions, 0));
        return permissions;
    }

    private void collect(Role role, Set<String> into, int depth) {
        if (role == null || depth > MAX_INHERITANCE_DEPTH) {
            return;
        }
        Set<Permission> perms = role.getPermissions();
        if (perms != null) {
            perms.forEach(p -> into.add(p.getName()));
        }
        if (role.getParent() != null) {
            collect(role.getParent(), into, depth + 1);
        }
    }

    /** Whether an effective permission set grants {@code resource:action}, honouring wildcards. */
    public boolean grants(Set<String> permissions, String resource, String action) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        return permissions.contains(resource + ":" + action)
                || permissions.contains(resource + ":*")
                || permissions.contains("*:*")
                || permissions.contains("*");
    }

    /** Union of assignment- and token-role-derived permissions. */
    public Set<String> effectivePermissions(String identityId, Set<String> tokenRoleNames) {
        Set<String> result = new HashSet<>(assignmentPermissions(identityId));
        if (tokenRoleNames != null) {
            for (String roleName : tokenRoleNames) {
                result.addAll(permissionsForRole(roleName));
            }
        }
        return result;
    }
}
