package com.paymentprocessor.notificationservice.dto;

public class TemplatePreviewResponse {
    private final String subject;
    private final String body;

    public TemplatePreviewResponse(String subject, String body) {
        this.subject = subject;
        this.body = body;
    }

    public String getSubject() { return subject; }
    public String getBody() { return body; }
}
