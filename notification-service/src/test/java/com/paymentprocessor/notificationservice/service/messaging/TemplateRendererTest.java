package com.paymentprocessor.notificationservice.service.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    @Test
    void substitutesKnownVariables() {
        String result = renderer.render(
                "Hi {{customer_name}}, your payment of {{amount}} was received.",
                Map.of("customer_name", "Ada", "amount", "$42.00"));

        assertThat(result).isEqualTo("Hi Ada, your payment of $42.00 was received.");
    }

    @Test
    void leavesUnknownVariablesUntouchedRatherThanThrowing() {
        String result = renderer.render("Hello {{name}}, id={{missing}}", Map.of("name", "Ada"));

        assertThat(result).isEqualTo("Hello Ada, id={{missing}}");
    }

    @Test
    void handlesNullTemplateAndNullVariables() {
        assertThat(renderer.render(null, Map.of())).isNull();
        assertThat(renderer.render("no variables here", null)).isEqualTo("no variables here");
    }

    @Test
    void treatsDollarSignsInValuesAsLiteral() {
        // Matcher.quoteReplacement in the implementation guards against '$' / '\' in
        // replacement values being misinterpreted as regex backreferences.
        String result = renderer.render("Total: {{amount}}", Map.of("amount", "$1,000"));

        assertThat(result).isEqualTo("Total: $1,000");
    }
}
