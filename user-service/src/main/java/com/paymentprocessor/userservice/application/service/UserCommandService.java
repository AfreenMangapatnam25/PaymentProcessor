package com.paymentprocessor.userservice.application.service;

import com.paymentprocessor.userservice.application.command.CreateUserCommand;
import com.paymentprocessor.userservice.application.command.UpdateUserProfileCommand;
import com.paymentprocessor.userservice.application.command.UpdateUserStatusCommand;
import com.paymentprocessor.userservice.application.port.out.OutboxPort;
import com.paymentprocessor.userservice.common.constants.ErrorCode;
import com.paymentprocessor.userservice.common.exception.ConflictException;
import com.paymentprocessor.userservice.common.exception.ValidationException;
import com.paymentprocessor.userservice.common.id.IdGenerator;
import com.paymentprocessor.userservice.common.util.ClockProvider;
import com.paymentprocessor.userservice.application.validator.UserValidator;
import com.paymentprocessor.userservice.domain.event.UserCreatedEvent;
import com.paymentprocessor.userservice.domain.event.UserStatusChangedEvent;
import com.paymentprocessor.userservice.domain.exception.UserNotFoundException;
import com.paymentprocessor.userservice.domain.repository.UserRepository;
import com.paymentprocessor.userservice.domain.user.User;
import com.paymentprocessor.userservice.domain.user.UserProfile;
import com.paymentprocessor.userservice.domain.user.UserStatus;
import com.paymentprocessor.userservice.domain.valueobject.Email;
import com.paymentprocessor.userservice.domain.valueobject.IdentityId;
import com.paymentprocessor.userservice.domain.valueobject.PhoneNumber;
import com.paymentprocessor.userservice.domain.valueobject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.Locale;

/**
 * Orchestrates user write use-cases. Holds NO business rules of its own (those
 * are in the domain, rule 4); it coordinates the aggregate, uniqueness checks,
 * persistence, and event emission. Each method is a single transaction so the
 * state change and its outbox event commit atomically (rule 11).
 */
@Service
public class UserCommandService {

    private final UserRepository userRepository;
    private final UserValidator userValidator;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;
    private final OutboxPort outbox;

    public UserCommandService(UserRepository userRepository,
                              UserValidator userValidator,
                              IdGenerator idGenerator,
                              ClockProvider clock,
                              OutboxPort outbox) {
        this.userRepository = userRepository;
        this.userValidator = userValidator;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.outbox = outbox;
    }

    @Transactional
    public User createUser(CreateUserCommand cmd) {
        IdentityId identityId = IdentityId.of(cmd.identityId());
        Email email = Email.of(cmd.email());
        userValidator.validateNewUser(identityId, email);

        UserProfile profile = buildProfile(email, cmd.firstName(), cmd.lastName(),
                cmd.dateOfBirth(), cmd.phone(), cmd.locale(), cmd.timezone());
        UserId id = UserId.of(idGenerator.generateUserId());
        User user = User.register(id, identityId, profile, clock.now());

        User saved = userRepository.save(user);
        outbox.append(new UserCreatedEvent(
                saved.getId().value(), saved.getIdentityId().value(),
                saved.getStatus().name(), clock.now()));
        return saved;
    }

    @Transactional
    public User changeStatus(UpdateUserStatusCommand cmd) {
        User user = loadForWrite(cmd.userId(), cmd.expectedVersion());
        UserStatus previous = user.getStatus();
        switch (cmd.action()) {
            case ACTIVATE -> user.activate();
            case SUSPEND -> user.suspend();
            case LOCK -> user.lock();
        }
        User saved = userRepository.save(user);
        if (previous != saved.getStatus()) {
            outbox.append(new UserStatusChangedEvent(
                    saved.getId().value(), previous.name(), saved.getStatus().name(), clock.now()));
        }
        return saved;
    }

    @Transactional
    public User updateProfile(UpdateUserProfileCommand cmd) {
        User user = loadForWrite(cmd.userId(), cmd.expectedVersion());
        Email email = Email.of(cmd.email());

        boolean emailChanged = user.getProfile() == null
                || !email.value().equals(user.getProfile().getEmail().value());
        if (emailChanged) {
            userValidator.validateEmailAvailable(email);
        }

        UserProfile profile = buildProfile(email, cmd.firstName(), cmd.lastName(),
                cmd.dateOfBirth(), cmd.phone(), cmd.locale(), cmd.timezone());
        user.updateProfile(profile);
        return userRepository.save(user);
    }

    private User loadForWrite(String userId, long expectedVersion) {
        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getVersion() != expectedVersion) {
            throw new ConflictException(ErrorCode.CONFLICT,
                    "User was modified concurrently; reload and retry");
        }
        return user;
    }

    private UserProfile buildProfile(Email email, String firstName, String lastName,
                                     java.time.LocalDate dateOfBirth, String phone,
                                     String locale, String timezone) {
        PhoneNumber phoneNumber = (phone != null && !phone.isBlank()) ? PhoneNumber.of(phone) : null;
        Locale parsedLocale = (locale != null && !locale.isBlank()) ? Locale.forLanguageTag(locale) : null;
        ZoneId parsedZone = parseZone(timezone);
        return new UserProfile(email, firstName, lastName, dateOfBirth, phoneNumber, parsedLocale, parsedZone);
    }

    private ZoneId parseZone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return null;
        }
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            throw new ValidationException("timezone", "Invalid timezone identifier");
        }
    }
}
