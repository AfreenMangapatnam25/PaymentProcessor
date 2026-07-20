package com.paymentprocessor.authorization.service.access;

import com.paymentprocessor.authorization.config.CacheConfig;
import com.paymentprocessor.authorization.domain.access.Permission;
import com.paymentprocessor.authorization.domain.access.Policy;
import com.paymentprocessor.authorization.domain.access.Role;
import com.paymentprocessor.authorization.domain.access.RoleAssignment;
import com.paymentprocessor.authorization.dto.access.PermissionRequest;
import com.paymentprocessor.authorization.dto.access.PolicyRequest;
import com.paymentprocessor.authorization.dto.access.RoleAssignmentRequest;
import com.paymentprocessor.authorization.dto.access.RoleRequest;
import com.paymentprocessor.authorization.event.AccessControlEvent;
import com.paymentprocessor.authorization.event.EventPublisher;
import com.paymentprocessor.authorization.exception.ResourceNotFoundException;
import com.paymentprocessor.authorization.exception.ValidationException;
import com.paymentprocessor.authorization.repository.PermissionRepository;
import com.paymentprocessor.authorization.repository.PolicyRepository;
import com.paymentprocessor.authorization.repository.RoleAssignmentRepository;
import com.paymentprocessor.authorization.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Administrative operations governing the authorization model: permissions, roles, role
 * assignments and policies. Mutations evict the permission caches and publish access-control
 * domain events for the Audit service and other consumers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RbacAdminService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleAssignmentRepository assignmentRepository;
    private final PolicyRepository policyRepository;
    private final EventPublisher eventPublisher;

    // ---------------------------------------------------------------- permissions

    @Transactional
    public Permission createPermission(PermissionRequest request) {
        if (permissionRepository.existsByName(request.name())) {
            throw new ValidationException("Permission already exists: " + request.name());
        }
        return permissionRepository.save(Permission.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .resource(request.resource())
                .action(request.action())
                .description(request.description())
                .build());
    }

    @Transactional(readOnly = true)
    public List<Permission> listPermissions() {
        return permissionRepository.findAll();
    }

    // ---------------------------------------------------------------- roles

    @Transactional
    @CacheEvict(cacheNames = {CacheConfig.EFFECTIVE_PERMISSIONS, CacheConfig.ROLE_PERMISSIONS}, allEntries = true)
    public Role createRole(RoleRequest request) {
        if (roleRepository.existsByName(request.name())) {
            throw new ValidationException("Role already exists: " + request.name());
        }
        Role role = Role.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .description(request.description())
                .category(request.category())
                .systemRole(request.systemRole())
                .permissions(resolvePermissions(request.permissions()))
                .build();
        if (request.parentRole() != null && !request.parentRole().isBlank()) {
            role.setParent(requireRole(request.parentRole()));
        }
        return roleRepository.save(role);
    }

    @Transactional(readOnly = true)
    public Role getRole(String name) {
        return requireRole(name);
    }

    @Transactional(readOnly = true)
    public List<Role> listRoles() {
        return roleRepository.findAll();
    }

    @Transactional
    @CacheEvict(cacheNames = {CacheConfig.EFFECTIVE_PERMISSIONS, CacheConfig.ROLE_PERMISSIONS}, allEntries = true)
    public Role grantPermission(String roleName, String permissionName) {
        Role role = requireRole(roleName);
        Permission permission = requirePermission(permissionName);
        role.getPermissions().add(permission);
        Role saved = roleRepository.save(role);
        eventPublisher.publishAccessControlEvent(AccessControlEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(AccessControlEvent.Types.PERMISSION_GRANTED)
                .roleName(roleName)
                .permissionName(permissionName)
                .occurredAt(Instant.now())
                .build());
        return saved;
    }

    @Transactional
    @CacheEvict(cacheNames = {CacheConfig.EFFECTIVE_PERMISSIONS, CacheConfig.ROLE_PERMISSIONS}, allEntries = true)
    public Role revokePermission(String roleName, String permissionName) {
        Role role = requireRole(roleName);
        role.getPermissions().removeIf(p -> p.getName().equals(permissionName));
        Role saved = roleRepository.save(role);
        eventPublisher.publishAccessControlEvent(AccessControlEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(AccessControlEvent.Types.PERMISSION_REVOKED)
                .roleName(roleName)
                .permissionName(permissionName)
                .occurredAt(Instant.now())
                .build());
        return saved;
    }

    @Transactional
    @CacheEvict(cacheNames = {CacheConfig.EFFECTIVE_PERMISSIONS, CacheConfig.ROLE_PERMISSIONS}, allEntries = true)
    public void deleteRole(String name) {
        Role role = requireRole(name);
        if (role.isSystemRole()) {
            throw new ValidationException("System roles cannot be deleted: " + name);
        }
        if (!assignmentRepository.findByRoleId(role.getId()).isEmpty()) {
            throw new ValidationException("Role has active assignments and cannot be deleted: " + name);
        }
        roleRepository.delete(role);
    }

    // ---------------------------------------------------------------- assignments

    @Transactional
    @CacheEvict(cacheNames = {CacheConfig.EFFECTIVE_PERMISSIONS, CacheConfig.ROLE_PERMISSIONS}, allEntries = true)
    public RoleAssignment assignRole(RoleAssignmentRequest request) {
        Role role = requireRole(request.roleName());
        RoleAssignment assignment = assignmentRepository.save(RoleAssignment.builder()
                .id(UUID.randomUUID())
                .identityId(request.identityId())
                .role(role)
                .scope(request.scope())
                .scopeId(request.scopeId())
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
                .build());
        eventPublisher.publishAccessControlEvent(AccessControlEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(AccessControlEvent.Types.ROLE_ASSIGNED)
                .identityId(request.identityId())
                .roleName(request.roleName())
                .scope(request.scope().name())
                .scopeId(request.scopeId())
                .occurredAt(Instant.now())
                .build());
        log.info("Assigned role {} to identity {} (scope={})",
                request.roleName(), request.identityId(), request.scope());
        return assignment;
    }

    @Transactional
    @CacheEvict(cacheNames = {CacheConfig.EFFECTIVE_PERMISSIONS, CacheConfig.ROLE_PERMISSIONS}, allEntries = true)
    public void revokeRole(String identityId, String roleName) {
        Role role = requireRole(roleName);
        assignmentRepository.deleteByIdentityIdAndRoleId(identityId, role.getId());
        eventPublisher.publishAccessControlEvent(AccessControlEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(AccessControlEvent.Types.ROLE_REVOKED)
                .identityId(identityId)
                .roleName(roleName)
                .occurredAt(Instant.now())
                .build());
    }

    @Transactional(readOnly = true)
    public List<RoleAssignment> listAssignments(String identityId) {
        return assignmentRepository.findByIdentityId(identityId);
    }

    // ---------------------------------------------------------------- policies

    @Transactional
    public Policy createPolicy(PolicyRequest request) {
        if (policyRepository.findByName(request.name()).isPresent()) {
            throw new ValidationException("Policy already exists: " + request.name());
        }
        return policyRepository.save(toPolicy(new Policy(), request, UUID.randomUUID()));
    }

    @Transactional
    public Policy updatePolicy(UUID id, PolicyRequest request) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + id));
        return policyRepository.save(toPolicy(policy, request, id));
    }

    @Transactional(readOnly = true)
    public List<Policy> listPolicies() {
        return policyRepository.findAll();
    }

    @Transactional
    public void deletePolicy(UUID id) {
        if (!policyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Policy not found: " + id);
        }
        policyRepository.deleteById(id);
    }

    // ---------------------------------------------------------------- helpers

    private Policy toPolicy(Policy policy, PolicyRequest request, UUID id) {
        policy.setId(id);
        policy.setName(request.name());
        policy.setType(request.type());
        policy.setEffect(request.effect());
        policy.setResource(request.resource());
        policy.setAction(request.action());
        policy.setConditionJson(request.conditionJson());
        policy.setPriority(request.priority());
        policy.setEnabled(request.enabled());
        return policy;
    }

    private Set<Permission> resolvePermissions(List<String> names) {
        Set<Permission> permissions = new HashSet<>();
        if (names != null) {
            for (String name : names) {
                permissions.add(requirePermission(name));
            }
        }
        return permissions;
    }

    private Role requireRole(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + name));
    }

    private Permission requirePermission(String name) {
        return permissionRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + name));
    }
}
