package com.paymentprocessor.notificationservice.service;

import com.paymentprocessor.notificationservice.entity.Message;
import com.paymentprocessor.notificationservice.repository.MessageRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * @deprecated Superseded by {@link com.paymentprocessor.notificationservice.service.messaging.MessageService},
 * which owns suppression checks, template rendering, and provider dispatch --
 * the actual "send a message" business logic. This class predates that and
 * is unused by any controller; it's kept only because generated code in this
 * environment can't be deleted outright. Safe to delete this file locally.
 * Explicit bean name below avoids colliding with the class of the same
 * simple name in the messaging package.
 */
@Deprecated
@Service("legacyMessageCrudService")
public class MessageService {

    private final MessageRepository repository;

    public MessageService(MessageRepository repository) {
        this.repository = repository;
    }

    public List<Message> findAll() {
        return repository.findAll();
    }

    public Optional<Message> findById(String id) {
        return repository.findById(id);
    }

    public Message save(Message entity) {
        return repository.save(entity);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
