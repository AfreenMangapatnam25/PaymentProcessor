package com.paymentprocessor.notificationservice.service;

import com.paymentprocessor.notificationservice.entity.Template;
import com.paymentprocessor.notificationservice.repository.TemplateRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * @deprecated Superseded by {@link com.paymentprocessor.notificationservice.service.messaging.TemplateService},
 * which enforces append-only versioning (POST always creates a new version
 * rather than overwriting one via save()). This class predates that and is
 * unused by any controller; it's kept only because generated code in this
 * environment can't be deleted outright. Safe to delete this file locally.
 * Explicit bean name below avoids colliding with the class of the same
 * simple name in the messaging package.
 */
@Deprecated
@Service("legacyTemplateCrudService")
public class TemplateService {

    private final TemplateRepository repository;

    public TemplateService(TemplateRepository repository) {
        this.repository = repository;
    }

    public List<Template> findAll() {
        return repository.findAll();
    }

    public Optional<Template> findById(String id) {
        return repository.findById(id);
    }

    public Template save(Template entity) {
        return repository.save(entity);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
