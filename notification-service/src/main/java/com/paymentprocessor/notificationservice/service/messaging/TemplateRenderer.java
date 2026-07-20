package com.paymentprocessor.notificationservice.service.messaging;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Minimal {{variable}} substitution -- deliberately not a full templating
 * engine (Mustache/Handlebars). Notification bodies are short, operator-
 * authored strings with a handful of named variables (amount, merchant_name,
 * etc.), so a regex substitution keeps this dependency-free while covering
 * the actual use case. Unresolved variables are left as-is rather than
 * throwing, so a missing/renamed variable fails loudly in a rendered
 * message review rather than blocking the send.
 */
@Component
public class TemplateRenderer {

    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.]+)\\s*}}");

    public String render(String template, Map<String, Object> variables) {
        if (template == null) {
            return null;
        }
        Matcher matcher = VARIABLE.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = variables == null ? null : variables.get(key);
            String replacement = value == null ? matcher.group(0) : Matcher.quoteReplacement(String.valueOf(value));
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
