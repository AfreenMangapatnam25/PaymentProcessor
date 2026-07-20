package com.paymentprocessor.notificationservice.dto;

import com.paymentprocessor.notificationservice.entity.Template;
import java.time.Instant;

public class TemplateResponse {
    private String id;
    private String key;
    private String channel;
    private String locale;
    private String subject;
    private String body;
    private int version;
    private Instant createdAt;

    public static TemplateResponse from(Template t) {
        TemplateResponse r = new TemplateResponse();
        r.id = t.getId();
        r.key = t.getKey();
        r.channel = t.getChannel();
        r.locale = t.getLocale();
        r.subject = t.getSubject();
        r.body = t.getBody();
        r.version = t.getVersion();
        r.createdAt = t.getCreatedAt();
        return r;
    }

    public String getId() { return id; }
    public String getKey() { return key; }
    public String getChannel() { return channel; }
    public String getLocale() { return locale; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public int getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
