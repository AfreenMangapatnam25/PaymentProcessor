package com.paymentprocessor.authenticationservice.service;

import com.paymentprocessor.authenticationservice.domain.IdentityStatus;
import com.paymentprocessor.authenticationservice.dto.RegisterIdentityRequest;
import com.paymentprocessor.authenticationservice.entity.Identity;
import com.paymentprocessor.authenticationservice.exception.ConflictException;
import com.paymentprocessor.authenticationservice.exception.NotFoundException;
import com.paymentprocessor.authenticationservice.repository.IdentityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class IdentityService {

    private final IdentityRepository repository;
    private final CredentialService credentialService;

    public IdentityService(IdentityRepository repository, CredentialService credentialService) {
        this.repository = repository;
        this.credentialService = credentialService;
    }

    @Transactional
    public Identity register(RegisterIdentityRequest req) {
        if (req.email() != null && repository.existsByEmailIgnoreCase(req.email())) {
            throw new ConflictException("An identity with this email already exists");
        }
        Identity identity = new Identity();
        identity.setId(UUID.randomUUID().toString());
        identity.setPrincipalType(req.principalType());
        identity.setEmail(req.email());
        identity.setPhoneE164(req.phoneE164());
        identity.setStatus(IdentityStatus.ACTIVE);
        identity.setMfaRequired(req.mfaRequired());
        Identity saved = repository.save(identity);
        credentialService.setPassword(saved.getId(), req.password());
        return saved;
    }

    @Transactional(readOnly = true)
    public Identity getById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Identity not found"));
    }

    @Transactional(readOnly = true)
    public Optional<Identity> findByEmail(String email) {
        return repository.findByEmailIgnoreCase(email);
    }

    @Transactional
    public Identity save(Identity identity) {
        return repository.save(identity);
    }

    @Transactional
    public void setStatus(String id, IdentityStatus status) {
        Identity identity = getById(id);
        identity.setStatus(status);
        repository.save(identity);
    }
}
