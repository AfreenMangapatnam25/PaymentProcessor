package com.paymentprocessor.userservice.api.controller;

import com.paymentprocessor.userservice.api.request.CreateCustomerRequest;
import com.paymentprocessor.userservice.api.request.UpdateCustomerRequest;
import com.paymentprocessor.userservice.api.response.ApiResponse;
import com.paymentprocessor.userservice.api.response.CustomerResponse;
import com.paymentprocessor.userservice.api.response.PageResponse;
import com.paymentprocessor.userservice.application.command.CreateCustomerCommand;
import com.paymentprocessor.userservice.application.command.DeleteCustomerCommand;
import com.paymentprocessor.userservice.application.command.EraseCustomerCommand;
import com.paymentprocessor.userservice.application.command.UpdateCustomerCommand;
import com.paymentprocessor.userservice.application.mapper.CustomerMapper;
import com.paymentprocessor.userservice.application.query.GetCustomerQuery;
import com.paymentprocessor.userservice.application.query.SearchCustomerQuery;
import com.paymentprocessor.userservice.application.service.CustomerCommandService;
import com.paymentprocessor.userservice.application.service.CustomerQueryService;
import com.paymentprocessor.userservice.common.constants.ApiConstants;
import com.paymentprocessor.userservice.common.exception.ForbiddenException;
import com.paymentprocessor.userservice.common.model.RequestContextFilter;
import com.paymentprocessor.userservice.domain.customer.Customer;
import com.paymentprocessor.userservice.infrastructure.security.JwtUserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Merchant-facing customer endpoints. The merchant scope is resolved from the
 * caller's JWT (never trusted from the body/path), so a caller can only ever
 * touch its own customers (rule 12). Thin boundary: no business logic here.
 */
@RestController
@RequestMapping(ApiConstants.API_V1 + ApiConstants.CUSTOMERS)
public class CustomerController {

    private final CustomerCommandService commandService;
    private final CustomerQueryService queryService;
    private final CustomerMapper customerMapper;
    private final JwtUserContext jwtUserContext;
    private final RequestContextFilter contextFilter;

    public CustomerController(CustomerCommandService commandService,
                             CustomerQueryService queryService,
                             CustomerMapper customerMapper,
                             JwtUserContext jwtUserContext,
                             RequestContextFilter contextFilter) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.customerMapper = customerMapper;
        this.jwtUserContext = jwtUserContext;
        this.contextFilter = contextFilter;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> create(@Valid @RequestBody CreateCustomerRequest request) {
        String merchantId = requireMerchant();
        Customer customer = commandService.create(new CreateCustomerCommand(
                merchantId, request.email(), request.fullName(), request.phone(),
                request.externalRef(), request.userId(), request.metadata()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(customerMapper.toResponse(customer),
                        contextFilter.getCurrentContext(), HttpStatus.CREATED));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<ApiResponse<CustomerResponse>> get(@PathVariable String customerId) {
        Customer customer = queryService.getById(new GetCustomerQuery(customerId, requireMerchant()));
        return ResponseEntity.ok(ApiResponse.success(
                customerMapper.toResponse(customer), contextFilter.getCurrentContext()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        String merchantId = requireMerchant();
        SearchCustomerQuery query = new SearchCustomerQuery(merchantId, page, size);
        List<CustomerResponse> content = queryService.search(query).stream()
                .map(customerMapper::toResponse)
                .toList();
        long total = queryService.count(merchantId);
        PageResponse<CustomerResponse> body = PageResponse.of(content, total, query.page(), query.size());
        return ResponseEntity.ok(ApiResponse.success(body, contextFilter.getCurrentContext()));
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(
            @PathVariable String customerId,
            @Valid @RequestBody UpdateCustomerRequest request) {
        Customer customer = commandService.update(new UpdateCustomerCommand(
                customerId, requireMerchant(), request.email(), request.fullName(),
                request.phone(), request.metadata(), request.expectedVersion()));
        return ResponseEntity.ok(ApiResponse.success(
                customerMapper.toResponse(customer), contextFilter.getCurrentContext()));
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<ApiResponse<CustomerResponse>> delete(
            @PathVariable String customerId,
            @RequestParam @PositiveOrZero long expectedVersion) {
        Customer customer = commandService.delete(new DeleteCustomerCommand(
                customerId, requireMerchant(), expectedVersion));
        return ResponseEntity.ok(ApiResponse.success(
                customerMapper.toResponse(customer), contextFilter.getCurrentContext()));
    }

    @PostMapping("/{customerId}/erasure")
    public ResponseEntity<ApiResponse<CustomerResponse>> erase(
            @PathVariable String customerId,
            @RequestParam @PositiveOrZero long expectedVersion) {
        Customer customer = commandService.erase(new EraseCustomerCommand(
                customerId, requireMerchant(), expectedVersion));
        return ResponseEntity.ok(ApiResponse.success(
                customerMapper.toResponse(customer), contextFilter.getCurrentContext()));
    }

    private String requireMerchant() {
        return jwtUserContext.currentMerchantId()
                .orElseThrow(ForbiddenException::merchantContextRequired);
    }
}
