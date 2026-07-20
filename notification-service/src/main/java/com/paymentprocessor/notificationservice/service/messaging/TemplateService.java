package com.paymentprocessor.notificationservice.service.messaging;

import com.paymentprocessor.notificationservice.dto.TemplateRequest;
import com.paymentprocessor.notificationservice.entity.Template;
import com.paymentprocessor.notificationservice.repository.TemplateRepository;
import com.paymentprocessor.notificationservice.util.IdGenerator;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateService {

    private final TemplateRepository repository;

    public TemplateService(TemplateRepository repository) {
        this.repository = repository;
    }

    /** Creates a new version of (key, channel, locale); versions are append-only, never edited in place. */
    @Transactional
    public Template createVersion(TemplateRequest request) {
        int nextVersion = repository
                .findTopByKeyAndChannelAndLocaleOrderByVersionDesc(request.getKey(), request.getChannel(), request.getLocale())
                .map(t -> t.getVersion() + 1)
                .orElse(1);

        Template template = new Template();
        template.setId(IdGenerator.generate("tmpl"));
        template.setKey(request.getKey());
        template.setChannel(request.getChannel());
        template.setLocale(request.getLocale());
        template.setSubject(request.getSubject());
        template.setBody(request.getBody());
        template.setVersion(nextVersion);
        template.setCreatedAt(Instant.now());
        return repository.save(template);
    }

    public Template getLatest(String key, String channel, String locale) {
        return repository.findTopByKeyAndChannelAndLocaleOrderByVersionDesc(key, channel, locale)
                .orElseThrow(() -> new NoSuchElementException(
                        "no template for key=" + key + " channel=" + channel + " locale=" + locale));
    }

    public Optional<Template> findVersion(String key, String channel, String locale, Integer version) {
        return repository.findByKeyAndChannelAndLocaleAndVersion(key, channel, locale, version);
    }

    public List<Template> findAllVersions(String key, String channel) {
        return repository.findByKeyAndChannel(key, channel);
    }

    public List<Template> findAll() {
        return repository.findAll();
    }
}
