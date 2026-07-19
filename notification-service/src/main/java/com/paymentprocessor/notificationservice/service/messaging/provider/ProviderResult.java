package com.paymentprocessor.notificationservice.service.messaging.provider;

public class ProviderResult {

    private final boolean success;
    private final String providerRef;
    private final String error;

    private ProviderResult(boolean success, String providerRef, String error) {
        this.success = success;
        this.providerRef = providerRef;
        this.error = error;
    }

    public static ProviderResult success(String providerRef) {
        return new ProviderResult(true, providerRef, null);
    }

    public static ProviderResult failure(String error) {
        return new ProviderResult(false, null, error);
    }

    public boolean isSuccess() { return success; }
    public String getProviderRef() { return providerRef; }
    public String getError() { return error; }
}
