package com.paymentprocessor.notificationservice.controller;

import com.paymentprocessor.notificationservice.dto.TemplatePreviewRequest;
import com.paymentprocessor.notificationservice.dto.TemplatePreviewResponse;
import com.paymentprocessor.notificationservice.dto.TemplateRequest;
import com.paymentprocessor.notificationservice.dto.TemplateResponse;
import com.paymentprocessor.notificationservice.entity.Template;
import com.paymentprocessor.notificationservice.service.messaging.TemplateRenderer;
import com.paymentprocessor.notificationservice.service.messaging.TemplateService;
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
import org.springframework.web.bind.annotation.RestController;

/** Templates are append-only versioned rows keyed by (key, channel, locale); POST always creates a new version. */
@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService service;
    private final TemplateRenderer renderer;

    public TemplateController(TemplateService service, TemplateRenderer renderer) {
        this.service = service;
        this.renderer = renderer;
    }

    @GetMapping
    public List<TemplateResponse> all() {
        return service.findAll().stream().map(TemplateResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> create(@Valid @RequestBody TemplateRequest request) {
        TemplateResponse response = TemplateResponse.from(service.createVersion(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{key}/{channel}")
    public List<TemplateResponse> versions(@PathVariable String key, @PathVariable String channel) {
        return service.findAllVersions(key, channel).stream().map(TemplateResponse::from).toList();
    }

    @GetMapping("/{key}/{channel}/{locale}")
    public TemplateResponse latest(@PathVariable String key, @PathVariable String channel, @PathVariable String locale) {
        return TemplateResponse.from(service.getLatest(key, channel, locale));
    }

    @GetMapping("/{key}/{channel}/{locale}/{version}")
    public TemplateResponse version(@PathVariable String key, @PathVariable String channel,
                                     @PathVariable String locale, @PathVariable Integer version) {
        return service.findVersion(key, channel, locale, version)
                .map(TemplateResponse::from)
                .orElseThrow(() -> new NoSuchElementException(
                        "no template version " + version + " for " + key + "/" + channel + "/" + locale));
    }

    /** Renders the latest version of a template against sample variables without sending or persisting anything. */
    @PostMapping("/preview")
    public TemplatePreviewResponse preview(@Valid @RequestBody TemplatePreviewRequest request) {
        Template template = service.getLatest(request.getKey(), request.getChannel(), request.getLocale());
        return new TemplatePreviewResponse(
                renderer.render(template.getSubject(), request.getVariables()),
                renderer.render(template.getBody(), request.getVariables()));
    }
}
