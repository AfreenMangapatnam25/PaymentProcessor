package com.paymentprocessor.notificationservice.controller;

import com.paymentprocessor.notificationservice.dto.MessageResponse;
import com.paymentprocessor.notificationservice.dto.SendMessageRequest;
import com.paymentprocessor.notificationservice.service.messaging.MessageService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService service;

    public MessageController(MessageService service) {
        this.service = service;
    }

    /** Renders the requested template and sends over email or SMS, honoring the suppression list. */
    @PostMapping
    public ResponseEntity<MessageResponse> send(@Valid @RequestBody SendMessageRequest request) {
        MessageResponse response = MessageResponse.from(service.send(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<MessageResponse> list(@RequestParam(required = false) String channel,
                                       @RequestParam(required = false) String recipient,
                                       @RequestParam(defaultValue = "50") int limit) {
        var messages = (channel != null && recipient != null)
                ? service.findByRecipient(channel, recipient, limit)
                : service.findAll(limit);
        return messages.stream().map(MessageResponse::from).toList();
    }

    @GetMapping("/{id}")
    public MessageResponse get(@PathVariable String id) {
        return service.findById(id)
                .map(MessageResponse::from)
                .orElseThrow(() -> new NoSuchElementException("no message with id " + id));
    }
}
