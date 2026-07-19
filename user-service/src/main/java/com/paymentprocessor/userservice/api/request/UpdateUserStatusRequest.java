package com.paymentprocessor.userservice.api.request;

import com.paymentprocessor.userservice.application.command.UserStatusAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request to change a user's status. {@code expectedVersion} is the ETag the
 * client last read; the write fails if it is stale (optimistic locking).
 */
public record UpdateUserStatusRequest(

        @NotNull
        UserStatusAction action,

        @NotNull
        @PositiveOrZero
        Long expectedVersion
) {
}
