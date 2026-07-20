package com.paymentprocessor.notificationservice.service.messaging;

import com.paymentprocessor.notificationservice.dto.SendMessageRequest;
import com.paymentprocessor.notificationservice.entity.Message;
import com.paymentprocessor.notificationservice.entity.Template;
import com.paymentprocessor.notificationservice.repository.MessageRepository;
import com.paymentprocessor.notificationservice.service.messaging.provider.EmailProvider;
import com.paymentprocessor.notificationservice.service.messaging.provider.ProviderResult;
import com.paymentprocessor.notificationservice.service.messaging.provider.SmsProvider;
import com.paymentprocessor.notificationservice.util.IdGenerator;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renders a template and sends it over email or SMS, enforcing the
 * suppression list first and never persisting the raw recipient (only its
 * hash, via RecipientHasher).
 */
@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    public static final String STATUS_SENT = "sent";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_SUPPRESSED = "suppressed";

    private final MessageRepository messageRepository;
    private final TemplateService templateService;
    private final SuppressionService suppressionService;
    private final RecipientHasher recipientHasher;
    private final TemplateRenderer renderer;
    private final EmailProvider emailProvider;
    private final SmsProvider smsProvider;

    public MessageService(MessageRepository messageRepository,
                           TemplateService templateService,
                           SuppressionService suppressionService,
                           RecipientHasher recipientHasher,
                           TemplateRenderer renderer,
                           EmailProvider emailProvider,
                           SmsProvider smsProvider) {
        this.messageRepository = messageRepository;
        this.templateService = templateService;
        this.suppressionService = suppressionService;
        this.recipientHasher = recipientHasher;
        this.renderer = renderer;
        this.emailProvider = emailProvider;
        this.smsProvider = smsProvider;
    }

    public Optional<Message> findById(String id) {
        return messageRepository.findById(id);
    }

    public List<Message> findAll(int limit) {
        return messageRepository.findAll(PageRequest.of(0, Math.max(1, Math.min(limit, 500)))).getContent();
    }

    public List<Message> findByRecipient(String channel, String rawRecipient, int limit) {
        byte[] hash = recipientHasher.hash(channel, rawRecipient);
        return messageRepository.findByChannelAndRecipientHash(channel, hash, PageRequest.of(0, Math.max(1, Math.min(limit, 500))));
    }

    @Transactional
    public Message send(SendMessageRequest request) {
        byte[] recipientHash = recipientHasher.hash(request.getChannel(), request.getRecipient());

        Message message = new Message();
        message.setId(IdGenerator.generate("msg"));
        message.setChannel(request.getChannel());
        message.setRecipientHash(recipientHash);
        message.setLocale(request.getLocale());
        message.setCreatedAt(Instant.now());

        if (suppressionService.isSuppressed(request.getChannel(), request.getRecipient())) {
            message.setStatus(STATUS_SUPPRESSED);
            return messageRepository.save(message);
        }

        Template template;
        try {
            template = templateService.getLatest(request.getTemplateKey(), request.getChannel(), request.getLocale());
        } catch (NoSuchElementException e) {
            message.setStatus(STATUS_FAILED);
            message.setTemplateId(null);
            return messageRepository.save(message);
        }
        message.setTemplateId(template.getId());

        Map<String, Object> variables = request.getVariables() != null ? request.getVariables() : new HashMap<>();
        String renderedBody = renderer.render(template.getBody(), variables);
        String renderedSubject = renderer.render(template.getSubject(), variables);

        ProviderResult result = dispatchToProvider(request, renderedSubject, renderedBody);

        message.setProvider(providerName(request.getChannel()));
        message.setProviderRef(result.getProviderRef());

        if (result.isSuccess()) {
            message.setStatus(STATUS_SENT);
            message.setSentAt(Instant.now());
        } else {
            message.setStatus(STATUS_FAILED);
            log.warn("failed to send {} message via {}: {}", request.getChannel(), providerName(request.getChannel()), result.getError());
        }

        return messageRepository.save(message);
    }

    private ProviderResult dispatchToProvider(SendMessageRequest request, String subject, String body) {
        if ("email".equalsIgnoreCase(request.getChannel())) {
            return emailProvider.send(request.getRecipient(), subject, body);
        }
        if ("sms".equalsIgnoreCase(request.getChannel())) {
            return smsProvider.send(request.getRecipient(), body);
        }
        return ProviderResult.failure("unsupported channel: " + request.getChannel());
    }

    private String providerName(String channel) {
        if ("email".equalsIgnoreCase(channel)) return emailProvider.name();
        if ("sms".equalsIgnoreCase(channel)) return smsProvider.name();
        return "unknown";
    }
}
