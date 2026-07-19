package com.paymentprocessor.userservice.infrastructure.persistence.adapter;

import com.paymentprocessor.userservice.common.constants.ErrorCode;
import com.paymentprocessor.userservice.common.exception.ConflictException;
import com.paymentprocessor.userservice.common.exception.InfrastructureException;
import com.paymentprocessor.userservice.common.util.ClockProvider;
import com.paymentprocessor.userservice.domain.repository.UserRepository;
import com.paymentprocessor.userservice.domain.user.User;
import com.paymentprocessor.userservice.domain.user.UserProfile;
import com.paymentprocessor.userservice.domain.user.UserStatus;
import com.paymentprocessor.userservice.domain.valueobject.Email;
import com.paymentprocessor.userservice.domain.valueobject.IdentityId;
import com.paymentprocessor.userservice.domain.valueobject.PhoneNumber;
import com.paymentprocessor.userservice.domain.valueobject.UserId;
import com.paymentprocessor.userservice.infrastructure.encryption.BlindIndexService;
import com.paymentprocessor.userservice.infrastructure.encryption.DataKeyService;
import com.paymentprocessor.userservice.infrastructure.encryption.EncryptionService;
import com.paymentprocessor.userservice.infrastructure.encryption.IssuedDataKey;
import com.paymentprocessor.userservice.infrastructure.persistence.entity.UserEntity;
import com.paymentprocessor.userservice.infrastructure.persistence.entity.UserProfileEntity;
import com.paymentprocessor.userservice.infrastructure.persistence.repository.UserJpaRepository;
import com.paymentprocessor.userservice.infrastructure.persistence.repository.UserProfileJpaRepository;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDate;
import java.util.Locale;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Adapter implementing the {@link UserRepository} domain port. Owns the
 * translation between the domain aggregate and the {@code users}/{@code
 * user_profiles} tables, including envelope encryption of PII and blind-index
 * computation. The domain never sees an entity or a byte of ciphertext.
 *
 * <p>Runs inside the calling application service's transaction. Optimistic
 * locking: on update the caller's expected {@code version} is compared to the
 * stored version (client lost-update protection) and JPA's {@code @Version}
 * additionally guards concurrent transactions.
 */
@Component
public class UserPersistenceAdapter implements UserRepository {

    private static final String SUBJECT_TYPE = "USER";

    private final UserJpaRepository userJpa;
    private final UserProfileJpaRepository profileJpa;
    private final DataKeyService dataKeyService;
    private final EncryptionService encryption;
    private final BlindIndexService blindIndex;
    private final ClockProvider clock;

    public UserPersistenceAdapter(UserJpaRepository userJpa,
                                  UserProfileJpaRepository profileJpa,
                                  DataKeyService dataKeyService,
                                  EncryptionService encryption,
                                  BlindIndexService blindIndex,
                                  ClockProvider clock) {
        this.userJpa = userJpa;
        this.profileJpa = profileJpa;
        this.dataKeyService = dataKeyService;
        this.encryption = encryption;
        this.blindIndex = blindIndex;
        this.clock = clock;
    }

    @Override
    public User save(User user) {
        String id = user.getId().value();
        return userJpa.findById(id)
                .map(existing -> update(existing, user))
                .orElseGet(() -> insert(user));
    }

    private User insert(User user) {
        String id = user.getId().value();
        IssuedDataKey issued = dataKeyService.issueFor(SUBJECT_TYPE, id);

        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setIdentityId(user.getIdentityId().value());
        entity.setStatus(user.getStatus().name());
        entity.setCryptoKeyId(issued.cryptoKeyId());
        entity.setErasedAt(user.getErasedAt());
        entity.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt() : clock.now());
        entity.setUpdatedAt(clock.now());
        UserEntity savedUser = userJpa.save(entity);

        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(id);
        writeProfile(profile, user.getProfile(), issued.dek());
        profile.setUpdatedAt(clock.now());
        profileJpa.save(profile);

        return user.toBuilder().version(savedUser.getVersion()).build();
    }

    private User update(UserEntity entity, User user) {
        if (user.getVersion() != entity.getVersion()) {
            throw new ConflictException(ErrorCode.CONFLICT,
                    "User was modified concurrently; reload and retry");
        }
        entity.setStatus(user.getStatus().name());
        entity.setErasedAt(user.getErasedAt());

        UserProfileEntity profile = profileJpa.findById(entity.getId())
                .orElseGet(() -> {
                    UserProfileEntity p = new UserProfileEntity();
                    p.setUserId(entity.getId());
                    return p;
                });

        if (user.isErased() || user.getProfile() == null) {
            // Crypto-shred cleanup: drop ciphertext and release blind indexes.
            clearProfile(profile);
        } else {
            SecretKey dek = dataKeyService.loadActiveKey(entity.getCryptoKeyId())
                    .orElseThrow(() -> new InfrastructureException(
                            ErrorCode.ENCRYPTION_ERROR, "Active data key unavailable for user"));
            writeProfile(profile, user.getProfile(), dek);
        }
        profile.setUpdatedAt(clock.now());
        profileJpa.save(profile);

        UserEntity saved = userJpa.saveAndFlush(entity);
        return user.toBuilder().version(saved.getVersion()).build();
    }

    @Override
    public Optional<User> findById(UserId id) {
        return userJpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<User> findByIdentityId(IdentityId identityId) {
        return userJpa.findByIdentityId(identityId.value()).map(this::toDomain);
    }

    @Override
    public boolean existsByIdentityId(IdentityId identityId) {
        return userJpa.existsByIdentityId(identityId.value());
    }

    @Override
    public boolean existsByEmail(Email email) {
        return profileJpa.existsByEmailIndex(blindIndex.index(email.value()));
    }

    // ----- mapping helpers -------------------------------------------------

    private void writeProfile(UserProfileEntity entity, UserProfile profile, SecretKey dek) {
        entity.setEmailEncrypted(encryption.encryptString(profile.getEmail().value(), dek));
        entity.setEmailIndex(blindIndex.index(profile.getEmail().value()));
        entity.setFirstNameEncrypted(encryption.encryptString(profile.getFirstName(), dek));
        entity.setLastNameEncrypted(encryption.encryptString(profile.getLastName(), dek));
        entity.setDateOfBirthEncrypted(encryption.encryptString(profile.getDateOfBirth().toString(), dek));
        if (profile.getPhone() != null) {
            entity.setPhoneEncrypted(encryption.encryptString(profile.getPhone().value(), dek));
            entity.setPhoneIndex(blindIndex.index(profile.getPhone().value()));
        } else {
            entity.setPhoneEncrypted(null);
            entity.setPhoneIndex(null);
        }
        entity.setLocale(profile.getLocale() != null ? profile.getLocale().toLanguageTag() : null);
        entity.setTimezone(profile.getTimezone() != null ? profile.getTimezone().getId() : null);
    }

    private void clearProfile(UserProfileEntity entity) {
        entity.setEmailEncrypted(null);
        entity.setEmailIndex(null);
        entity.setFirstNameEncrypted(null);
        entity.setLastNameEncrypted(null);
        entity.setDateOfBirthEncrypted(null);
        entity.setPhoneEncrypted(null);
        entity.setPhoneIndex(null);
    }

    private User toDomain(UserEntity entity) {
        UserStatus status = UserStatus.valueOf(entity.getStatus());
        UserProfile profile = null;

        if (status != UserStatus.ERASED) {
            Optional<SecretKey> dek = dataKeyService.loadActiveKey(entity.getCryptoKeyId());
            if (dek.isPresent()) {
                UserProfileEntity p = profileJpa.findById(entity.getId()).orElse(null);
                if (p != null && p.getEmailEncrypted() != null) {
                    profile = decryptProfile(p, dek.get());
                }
            }
        }

        return User.builder()
                .id(UserId.of(entity.getId()))
                .identityId(IdentityId.of(entity.getIdentityId()))
                .status(status)
                .profile(profile)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .erasedAt(entity.getErasedAt())
                .version(entity.getVersion())
                .build();
    }

    private UserProfile decryptProfile(UserProfileEntity p, SecretKey dek) {
        Email email = Email.of(encryption.decryptToString(p.getEmailEncrypted(), dek));
        String firstName = encryption.decryptToString(p.getFirstNameEncrypted(), dek);
        String lastName = encryption.decryptToString(p.getLastNameEncrypted(), dek);
        LocalDate dob = LocalDate.parse(encryption.decryptToString(p.getDateOfBirthEncrypted(), dek));
        PhoneNumber phone = p.getPhoneEncrypted() != null
                ? PhoneNumber.of(encryption.decryptToString(p.getPhoneEncrypted(), dek))
                : null;
        Locale locale = p.getLocale() != null ? Locale.forLanguageTag(p.getLocale()) : null;
        ZoneId timezone = p.getTimezone() != null ? ZoneId.of(p.getTimezone()) : null;
        return new UserProfile(email, firstName, lastName, dob, phone, locale, timezone);
    }
}
