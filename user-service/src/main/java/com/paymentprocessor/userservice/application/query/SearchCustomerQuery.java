package com.paymentprocessor.userservice.application.query;

/**
 * Paged listing of a merchant's live customers.
 */
public record SearchCustomerQuery(String merchantId, int page, int size) {

    public SearchCustomerQuery {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 200) {
            size = 50;
        }
    }
}
