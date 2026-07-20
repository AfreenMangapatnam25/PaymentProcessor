package com.paymentprocessor.userservice.api.controller;

import com.paymentprocessor.userservice.api.request.CreateUserRequest;
import com.paymentprocessor.userservice.api.request.UpdateUserStatusRequest;
import com.paymentprocessor.userservice.api.response.ApiResponse;
import com.paymentprocessor.userservice.api.response.UserResponse;
import com.paymentprocessor.userservice.application.command.CreateUserCommand;
import com.paymentprocessor.userservice.application.command.EraseUserCommand;
import com.paymentprocessor.userservice.application.command.UpdateUserStatusCommand;
import com.paymentprocessor.userservice.application.mapper.UserMapper;
import com.paymentprocessor.userservice.application.query.GetUserQuery;
import com.paymentprocessor.userservice.application.service.GdprEraseService;
import com.paymentprocessor.userservice.application.service.UserCommandService;
import com.paymentprocessor.userservice.application.service.UserQueryService;
import com.paymentprocessor.userservice.common.constants.ApiConstants;
import com.paymentprocessor.userservice.common.model.RequestContextFilter;
import com.paymentprocessor.userservice.domain.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for the User aggregate. Controllers are a thin boundary
 * (rule 2): translate request DTOs to commands/queries, invoke the application
 * service, and map the resulting domain object to a response DTO. No business
 * logic, no persistence, no domain mutation here.
 */
@RestController
@RequestMapping(ApiConstants.API_V1 + ApiConstants.USERS)
public class UserController {

    private final UserCommandService commandService;
    private final UserQueryService queryService;
    private final GdprEraseService gdprEraseService;
    private final UserMapper userMapper;
    private final RequestContextFilter contextFilter;

    public UserController(UserCommandService commandService,
                          UserQueryService queryService,
                          GdprEraseService gdprEraseService,
                          UserMapper userMapper,
                          RequestContextFilter contextFilter) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.gdprEraseService = gdprEraseService;
        this.userMapper = userMapper;
        this.contextFilter = contextFilter;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        User user = commandService.createUser(new CreateUserCommand(
                request.identityId(), request.email(), request.firstName(), request.lastName(),
                request.dateOfBirth(), request.phone(), request.locale(), request.timezone()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userMapper.toResponse(user),
                        contextFilter.getCurrentContext(), HttpStatus.CREATED));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> get(@PathVariable String userId) {
        User user = queryService.getById(new GetUserQuery(userId));
        return ResponseEntity.ok(ApiResponse.success(
                userMapper.toResponse(user), contextFilter.getCurrentContext()));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<UserResponse>> changeStatus(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        User user = commandService.changeStatus(new UpdateUserStatusCommand(
                userId, request.action(), request.expectedVersion()));
        return ResponseEntity.ok(ApiResponse.success(
                userMapper.toResponse(user), contextFilter.getCurrentContext()));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> erase(
            @PathVariable String userId,
            @RequestParam @PositiveOrZero long expectedVersion) {
        User user = gdprEraseService.erase(new EraseUserCommand(userId, expectedVersion));
        return ResponseEntity.ok(ApiResponse.success(
                userMapper.toResponse(user), contextFilter.getCurrentContext()));
    }
}
