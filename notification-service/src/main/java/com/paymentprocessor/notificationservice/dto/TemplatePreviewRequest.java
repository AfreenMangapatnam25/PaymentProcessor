package com.paymentprocessor.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class TemplatePreviewRequest {

    @NotBlank
    private String key;

    @NotBlank
    private String channel;

    @NotBlank
    private String locale;

    private Map<String, Object> variables;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> variables) { this.variables = variables; }
}
