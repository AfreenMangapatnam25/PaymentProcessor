package com.paymentprocessor.authenticationservice.event;

import com.paymentprocessor.authenticationservice.event.AuthEvents.*;

/** Abstraction over the event transport so the flows do not depend on Kafka directly. */
public interface DomainEventPublisher {
    void publish(UserLoggedIn event);
    void publish(UserLoggedOut event);
    void publish(PasswordChanged event);
    void publish(MfaEnabled event);
    void publish(AccountLocked event);
    void publish(NewDeviceLogin event);
}
