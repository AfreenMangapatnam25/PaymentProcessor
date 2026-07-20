package com.paymentprocessor.notificationservice.service.webhook;

public class WebhookDeliveryResult {

    private final boolean success;
    private final Integer statusCode;
    private final int responseMs;
    private final String error;

    private WebhookDeliveryResult(boolean success, Integer statusCode, int responseMs, String error) {
        this.success = success;
        this.statusCode = statusCode;
        this.responseMs = responseMs;
        this.error = error;
    }

    public static WebhookDeliveryResult success(int statusCode, int responseMs) {
        return new WebhookDeliveryResult(true, statusCode, responseMs, null);
    }

    public static WebhookDeliveryResult httpFailure(int statusCode, int responseMs) {
        return new WebhookDeliveryResult(false, statusCode, responseMs, "unexpected status code " + statusCode);
    }

    public static WebhookDeliveryResult transportFailure(int responseMs, String error) {
        return new WebhookDeliveryResult(false, null, responseMs, error);
    }

    public boolean isSuccess() { return success; }
    public Integer getStatusCode() { return statusCode; }
    public int getResponseMs() { return responseMs; }
    public String getError() { return error; }
}
