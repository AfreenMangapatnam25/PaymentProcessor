package com.paymentprocessor.notificationservice.service;

import com.paymentprocessor.notificationservice.entity.Suppression;
import com.paymentprocessor.notificationservice.entity.SuppressionId;
import com.paymentprocessor.notificationservice.repository.SuppressionRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * @deprecated Superseded by {@link com.paymentprocessor.notificationservice.service.messaging.SuppressionService},
 * which hashes recipients before ever touching the repository (this class's
 * save() would require a caller to already have a recipient_hash, which
 * only RecipientHasher should produce). This class predates that and is
 * unused by any controller; it's kept only because generated code in this
 * environment can't be deleted outright. Safe to delete this file locally.
 * Also fixed here: the original stub's findById/deleteById took a Long,
 * left over from before suppressions.id was replaced by the composite
 * (channel, recipient_hash) key -- that no longer matches
 * SuppressionRepository's key type and wouldn't have compiled.
 * Explicit bean name below avoids colliding with the class of the same
 * simple name in the messaging package.
 */
@Deprecated
@Service("legacySuppressionCrudService")
public class SuppressionService {

    private final SuppressionRepository repository;

    public SuppressionService(SuppressionRepository repository) {
        this.repository = repository;
    }

    public List<Suppression> findAll() {
        return repository.findAll();
    }

    public Optional<Suppression> findById(SuppressionId id) {
        return repository.findById(id);
    }

    public Suppression save(Suppression entity) {
        return repository.save(entity);
    }

    public void deleteById(SuppressionId id) {
        repository.deleteById(id);
    }
}
