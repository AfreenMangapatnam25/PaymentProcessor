package com.paymentprocessor.userservice.api.controller;

import com.paymentprocessor.userservice.api.request.UpdateUserProfileRequest;
import com.paymentprocessor.userservice.api.response.ApiResponse;
import com.paymentprocessor.userservice.api.response.UserProfileResponse;
import com.paymentprocessor.userservice.application.command.UpdateUserProfileCommand;
import com.paymentprocessor.userservice.application.mapper.UserMapper;
import com.paymentprocessor.userservice.application.query.GetUserQuery;
import com.paymentprocessor.userservice.application.service.UserCommandService;
import com.paymentprocessor.userservice.application.service.UserQueryService;
import com.paymentprocessor.userservice.common.constants.ApiConstants;
import com.paymentprocessor.userservice.common.model.RequestContextFilter;
import com.paymentprocessor.userservice.domain.user.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Profile sub-resource of a user. Returns/updates the decrypted profile at the
 * boundary; returns a null profile body when the user has been erased.
 */
@RestController
@RequestMapping(ApiConstants.API_V1 + ApiConstants.USERS)
public class UserProfileController {

    private final UserCommandService commandService;
    private final UserQueryService queryService;
    private final UserMapper userMapper;
    private final RequestContextFilter contextFilter;

    public UserProfileController(UserCommandService commandService,
                                 UserQueryService queryService,
                                 UserMapper userMapper,
                                 RequestContextFilter contextFilter) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.userMapper = userMapper;
        this.contextFilter = contextFilter;
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(@PathVariable String userId) {
        User user = queryService.getById(new GetUserQuery(userId));
        UserProfileResponse body = user.getProfile() != null
                ? userMapper.toProfileResponse(user.getProfile())
                : null;
        return ResponseEntity.ok(ApiResponse.success(body, contextFilter.getCurrentContext()));
    }

    @PutMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        User user = commandService.updateProfile(new UpdateUserProfileCommand(
                userId, request.email(), request.firstName(), request.lastName(),
                request.dateOfBirth(), request.phone(), request.locale(),
                request.timezone(), request.expectedVersion()));
        return ResponseEntity.ok(ApiResponse.success(
                userMapper.toProfileResponse(user.getProfile()), contextFilter.getCurrentContext()));
    }
}
