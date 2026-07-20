package com.paymentprocessor.userservice.api.controller;

import com.paymentprocessor.userservice.api.response.ApiResponse;
import com.paymentprocessor.userservice.api.response.CustomerResponse;
import com.paymentprocessor.userservice.application.mapper.CustomerMapper;
import com.paymentprocessor.userservice.application.query.GetCustomerQuery;
import com.paymentprocessor.userservice.application.service.CustomerQueryService;
import com.paymentprocessor.userservice.common.constants.ApiConstants;
import com.paymentprocessor.userservice.common.constants.HeaderConstants;
import com.paymentprocessor.userservice.common.model.RequestContextFilter;
import com.paymentprocessor.userservice.domain.customer.Customer;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service customer lookups (e.g. payment-service refreshing its read
 * model). Callers must present the {@code internal.customers.read} scope and
 * pass the merchant explicitly; isolation still applies. Method security is
 * enforced via {@code @PreAuthorize}.
 */
@RestController
@RequestMapping(ApiConstants.API_V1 + ApiConstants.INTERNAL + ApiConstants.CUSTOMERS)
public class InternalCustomerController {

    private final CustomerQueryService queryService;
    private final CustomerMapper customerMapper;
    private final RequestContextFilter contextFilter;

    public InternalCustomerController(CustomerQueryService queryService,
                                      CustomerMapper customerMapper,
                                      RequestContextFilter contextFilter) {
        this.queryService = queryService;
        this.customerMapper = customerMapper;
        this.contextFilter = contextFilter;
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasAuthority('SCOPE_internal.customers.read')")
    public ResponseEntity<ApiResponse<CustomerResponse>> get(
            @PathVariable String customerId,
            @RequestHeader(HeaderConstants.X_MERCHANT_ID) String merchantId) {
        Customer customer = queryService.getById(new GetCustomerQuery(customerId, merchantId));
        return ResponseEntity.ok(ApiResponse.success(
                customerMapper.toResponse(customer), contextFilter.getCurrentContext()));
    }
}
