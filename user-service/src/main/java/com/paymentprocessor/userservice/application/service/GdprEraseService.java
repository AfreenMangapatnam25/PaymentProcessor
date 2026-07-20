package com.paymentprocessor.userservice.application.service;

import com.paymentprocessor.userservice.application.command.EraseUserCommand;
import com.paymentprocessor.userservice.application.port.out.CryptoShredderPort;
import com.paymentprocessor.userservice.application.port.out.OutboxPort;
import com.paymentprocessor.userservice.common.constants.ErrorCode;
import com.paymentprocessor.userservice.common.exception.ConflictException;
import com.paymentprocessor.userservice.common.util.ClockProvider;
import com.paymentprocessor.userservice.domain.event.UserErasedEvent;
import com.paymentprocessor.userservice.domain.exception.UserNotFoundException;
import com.paymentprocessor.userservice.domain.repository.UserRepository;
import com.paymentprocessor.userservice.domain.user.User;
import com.paymentprocessor.userservice.domain.valueobject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * GDPR erasure via crypto-shredding (rules 10-11). In one transaction we: move
 * the aggregate to the terminal ERASED state (dropping PII), persist that
 * (which nulls the ciphertext columns and releases blind indexes), destroy the
 * subject's DEK (making any residual ciphertext unrecoverable), and emit the
 * erasure event. Foreign keys from ledger/payment services stay intact -- no
 * row is deleted.
 */
@Service
public class GdprEraseService {

    private final UserRepository userRepository;
    private final CryptoShredderPort cryptoShredder;
    private final OutboxPort outbox;
    private final ClockProvider clock;

    public GdprEraseService(UserRepository userRepository,
                            CryptoShredderPort cryptoShredder,
                            OutboxPort outbox,
                            ClockProvider clock) {
        this.userRepository = userRepository;
        this.cryptoShredder = cryptoShredder;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public User erase(EraseUserCommand cmd) {
        User user = userRepository.findById(UserId.of(cmd.userId()))
                .orElseThrow(() -> new UserNotFoundException(cmd.userId()));
        if (user.getVersion() != cmd.expectedVersion()) {
            throw new ConflictException(ErrorCode.CONFLICT,
                    "User was modified concurrently; reload and retry");
        }

        Instant now = clock.now();
        user.erase(now);
        User saved = userRepository.save(user);
        cryptoShredder.shred("USER", cmd.userId());
        outbox.append(new UserErasedEvent(saved.getId().value(), now, now));
        return saved;
    }
}
