package com.paymentprocessor.userservice.api.controller;

import com.paymentprocessor.userservice.api.request.CreateAddressRequest;
import com.paymentprocessor.userservice.api.request.UpdateAddressRequest;
import com.paymentprocessor.userservice.api.response.AddressResponse;
import com.paymentprocessor.userservice.api.response.ApiResponse;
import com.paymentprocessor.userservice.api.response.PageResponse;
import com.paymentprocessor.userservice.application.command.CreateAddressCommand;
import com.paymentprocessor.userservice.application.command.DeleteAddressCommand;
import com.paymentprocessor.userservice.application.command.UpdateAddressCommand;
import com.paymentprocessor.userservice.application.mapper.AddressMapper;
import com.paymentprocessor.userservice.application.query.GetAddressQuery;
import com.paymentprocessor.userservice.application.query.GetCustomerQuery;
import com.paymentprocessor.userservice.application.query.ListAddressesQuery;
import com.paymentprocessor.userservice.application.service.AddressCommandService;
import com.paymentprocessor.userservice.application.service.AddressQueryService;
import com.paymentprocessor.userservice.application.service.CustomerQueryService;
import com.paymentprocessor.userservice.common.constants.ApiConstants;
import com.paymentprocessor.userservice.common.exception.ForbiddenException;
import com.paymentprocessor.userservice.common.model.RequestContextFilter;
import com.paymentprocessor.userservice.domain.address.Address;
import com.paymentprocessor.userservice.domain.address.OwnerType;
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
 * Address endpoints for both user- and customer-owned addresses. Ownership is
 * authorized before any operation: customer-owned addresses must belong to a
 * customer within the caller's merchant scope (verified via the merchant-scoped
 * customer query, which 404s otherwise), preserving tenant isolation (rule 12).
 * Thin boundary -- no business logic here.
 */
@RestController
@RequestMapping(ApiConstants.API_V1 + ApiConstants.ADDRESSES)
public class AddressController {

    private final AddressCommandService commandService;
    private final AddressQueryService queryService;
    private final CustomerQueryService customerQueryService;
    private final AddressMapper addressMapper;
    private final JwtUserContext jwtUserContext;
    private final RequestContextFilter contextFilter;

    public AddressController(AddressCommandService commandService,
                             AddressQueryService queryService,
                             CustomerQueryService customerQueryService,
                             AddressMapper addressMapper,
                             JwtUserContext jwtUserContext,
                             RequestContextFilter contextFilter) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.customerQueryService = customerQueryService;
        this.addressMapper = addressMapper;
        this.jwtUserContext = jwtUserContext;
        this.contextFilter = contextFilter;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> create(@Valid @RequestBody CreateAddressRequest request) {
        authorizeOwner(request.ownerType(), request.ownerId());
        Address address = commandService.create(new CreateAddressCommand(
                request.ownerType(), request.ownerId(), request.addressType(),
                request.line1(), request.line2(), request.city(), request.region(),
                request.postalCode(), request.countryCode(), request.defaultAddress()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(addressMapper.toResponse(address),
                        contextFilter.getCurrentContext(), HttpStatus.CREATED));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AddressResponse>>> list(
            @RequestParam OwnerType ownerType,
            @RequestParam String ownerId) {
        authorizeOwner(ownerType, ownerId);
        List<AddressResponse> content = queryService.list(new ListAddressesQuery(ownerType, ownerId))
                .stream().map(addressMapper::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.ofList(content), contextFilter.getCurrentContext()));
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> get(
            @PathVariable String addressId,
            @RequestParam OwnerType ownerType,
            @RequestParam String ownerId) {
        authorizeOwner(ownerType, ownerId);
        Address address = queryService.getById(new GetAddressQuery(addressId, ownerType, ownerId));
        return ResponseEntity.ok(ApiResponse.success(
                addressMapper.toResponse(address), contextFilter.getCurrentContext()));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> update(
            @PathVariable String addressId,
            @RequestParam OwnerType ownerType,
            @RequestParam String ownerId,
            @Valid @RequestBody UpdateAddressRequest request) {
        authorizeOwner(ownerType, ownerId);
        Address address = commandService.update(new UpdateAddressCommand(
                addressId, ownerType, ownerId, request.addressType(), request.line1(),
                request.line2(), request.city(), request.region(), request.postalCode(),
                request.countryCode(), request.defaultAddress(), request.expectedVersion()));
        return ResponseEntity.ok(ApiResponse.success(
                addressMapper.toResponse(address), contextFilter.getCurrentContext()));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> delete(
            @PathVariable String addressId,
            @RequestParam OwnerType ownerType,
            @RequestParam String ownerId,
            @RequestParam @PositiveOrZero long expectedVersion) {
        authorizeOwner(ownerType, ownerId);
        Address address = commandService.delete(new DeleteAddressCommand(
                addressId, ownerType, ownerId, expectedVersion));
        return ResponseEntity.ok(ApiResponse.success(
                addressMapper.toResponse(address), contextFilter.getCurrentContext()));
    }

    /**
     * Authorize the caller for the given owner. Customer-owned addresses are
     * only accessible within the caller's merchant scope; the merchant-scoped
     * customer lookup throws not-found if the customer is out of scope.
     */
    private void authorizeOwner(OwnerType ownerType, String ownerId) {
        if (ownerType == OwnerType.CUSTOMER) {
            String merchantId = jwtUserContext.currentMerchantId()
                    .orElseThrow(ForbiddenException::merchantContextRequired);
            customerQueryService.getById(new GetCustomerQuery(ownerId, merchantId));
        }
        // USER-owned addresses require authentication (enforced by SecurityConfig).
        // Stricter self/admin checks can be layered here without touching lower layers.
    }
}
