package com.paymentprocessor.gatewayservice.error;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

/**
 * Renders every unhandled error as a consistent JSON envelope, so clients never see
 * raw stack traces or downstream error pages. Ordered ahead of the default handler.
 */
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalErrorWebExceptionHandler(ErrorAttributes errorAttributes,
            WebProperties webProperties,
            ApplicationContext applicationContext,
            ServerCodecConfigurer codecConfigurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        super.setMessageWriters(codecConfigurer.getWriters());
        super.setMessageReaders(codecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(request -> true, this::renderError);
    }

    private Mono<ServerResponse> renderError(ServerRequest request) {
        Map<String, Object> attrs = getErrorAttributes(request,
                ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE));

        int statusCode = (int) attrs.getOrDefault("status", 500);
        HttpStatus status = HttpStatus.resolve(statusCode);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("error", status.getReasonPhrase().toLowerCase().replace(' ', '_'));
        body.put("message", attrs.getOrDefault("message", "Unexpected error"));
        body.put("status", status.value());
        body.put("path", attrs.get("path"));
        body.put("timestamp", attrs.get("timestamp"));
        String correlationId = request.headers().firstHeader("X-Correlation-Id");
        body.put("correlationId", correlationId != null ? correlationId : "");

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }
}
