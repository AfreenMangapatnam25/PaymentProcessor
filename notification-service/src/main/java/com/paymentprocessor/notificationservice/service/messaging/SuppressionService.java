package com.paymentprocessor.notificationservice.service.messaging;

import com.paymentprocessor.notificationservice.entity.Suppression;
import com.paymentprocessor.notificationservice.repository.SuppressionRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuppressionService {

    private final SuppressionRepository repository;
    private final RecipientHasher hasher;

    public SuppressionService(SuppressionRepository repository, RecipientHasher hasher) {
        this.repository = repository;
        this.hasher = hasher;
    }

    public boolean isSuppressed(String channel, String rawRecipient) {
        return repository.existsByIdChannelAndIdRecipientHash(channel, hasher.hash(channel, rawRecipient));
    }

    @Transactional
    public Suppression suppress(String channel, String rawRecipient, String reason) {
        Suppression suppression = new Suppression(channel, hasher.hash(channel, rawRecipient), reason);
        suppression.setCreatedAt(Instant.now());
        return repository.save(suppression);
    }

    public List<Suppression> findAll() {
        return repository.findAll();
    }
}
