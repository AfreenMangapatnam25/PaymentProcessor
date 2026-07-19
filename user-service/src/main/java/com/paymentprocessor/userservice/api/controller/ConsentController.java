package com.paymentprocessor.userservice.api.controller;

import com.paymentprocessor.userservice.api.request.GrantConsentRequest;
import com.paymentprocessor.userservice.api.response.ApiResponse;
import com.paymentprocessor.userservice.api.response.ConsentResponse;
import com.paymentprocessor.userservice.api.response.PageResponse;
import com.paymentprocessor.userservice.application.command.GrantConsentCommand;
import com.paymentprocessor.userservice.application.command.RevokeConsentCommand;
import com.paymentprocessor.userservice.application.mapper.ConsentMapper;
import com.paymentprocessor.userservice.application.query.GetConsentQuery;
import com.paymentprocessor.userservice.application.query.GetCustomerQuery;
import com.paymentprocessor.userservice.application.query.ListConsentsQuery;
import com.paymentprocessor.userservice.application.service.ConsentCommandService;
import com.paymentprocessor.userservice.application.service.ConsentQueryService;
import com.paymentprocessor.userservice.application.service.CustomerQueryService;
import com.paymentprocessor.userservice.common.constants.ApiConstants;
import com.paymentprocessor.userservice.common.exception.ForbiddenException;
import com.paymentprocessor.userservice.common.model.RequestContextFilter;
import com.paymentprocessor.userservice.domain.consent.Consent;
import com.paymentprocessor.userservice.domain.consent.ConsentKind;
import com.paymentprocessor.userservice.domain.consent.SubjectType;
import com.paymentprocessor.userservice.infrastructure.security.JwtUserContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consent endpoints for user- and customer-subjects. Customer-subject consents
 * are authorized within the caller's merchant scope (rule 12). Thin boundary --
 * no business logic here.
 */
@RestController
@RequestMapping(ApiConstants.API_V1 + ApiConstants.CONSENTS)
public class ConsentController {

    private final ConsentCommandService commandService;
    private final ConsentQueryService queryService;
    private final CustomerQueryService customerQueryService;
    private final ConsentMapper consentMapper;
    private final JwtUserContext jwtUserContext;
    private final RequestContextFilter contextFilter;

    public ConsentController(ConsentCommandService commandService,
                             ConsentQueryService queryService,
                             CustomerQueryService customerQueryService,
                             ConsentMapper consentMapper,
                             JwtUserContext jwtUserContext,
                             RequestContextFilter contextFilter) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.customerQueryService = customerQueryService;
        this.consentMapper = consentMapper;
        this.jwtUserContext = jwtUserContext;
        this.contextFilter = contextFilter;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConsentResponse>> grant(@Valid @RequestBody GrantConsentRequest request) {
        authorizeSubject(request.subjectType(), request.subjectId());
        Consent consent = commandService.grant(new GrantConsentCommand(
                request.subjectType(), request.subjectId(), request.kind(),
                request.source(), request.policyVersion()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(consentMapper.toResponse(consent),
                        contextFilter.getCurrentContext(), HttpStatus.CREATED));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<ConsentResponse>> revoke(
            @RequestParam SubjectType subjectType,
            @RequestParam String subjectId,
            @RequestParam ConsentKind kind) {
        authorizeSubject(subjectType, subjectId);
        Consent consent = commandService.revoke(new RevokeConsentCommand(subjectType, subjectId, kind));
        return ResponseEntity.ok(ApiResponse.success(
                consentMapper.toResponse(consent), contextFilter.getCurrentContext()));
    }

    @GetMapping("/{kind}")
    public ResponseEntity<ApiResponse<ConsentResponse>> get(
            @PathVariable ConsentKind kind,
            @RequestParam SubjectType subjectType,
            @RequestParam String subjectId) {
        authorizeSubject(subjectType, subjectId);
        Consent consent = queryService.get(new GetConsentQuery(subjectType, subjectId, kind));
        return ResponseEntity.ok(ApiResponse.success(
                consentMapper.toResponse(consent), contextFilter.getCurrentContext()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ConsentResponse>>> list(
            @RequestParam SubjectType subjectType,
            @RequestParam String subjectId) {
        authorizeSubject(subjectType, subjectId);
        List<ConsentResponse> content = queryService.list(new ListConsentsQuery(subjectType, subjectId))
                .stream().map(consentMapper::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.ofList(content), contextFilter.getCurrentContext()));
    }

    private void authorizeSubject(SubjectType subjectType, String subjectId) {
        if (subjectType == SubjectType.CUSTOMER) {
            String merchantId = jwtUserContext.currentMerchantId()
                    .orElseThrow(ForbiddenException::merchantContextRequired);
            customerQueryService.getById(new GetCustomerQuery(subjectId, merchantId));
        }
        // USER-subject consents require authentication (enforced by SecurityConfig).
    }
}
