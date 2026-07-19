package com.paymentprocessor.userservice.domain.user;

import com.paymentprocessor.userservice.common.exception.ValidationException;
import com.paymentprocessor.userservice.common.util.DateTimeUtils;
import com.paymentprocessor.userservice.domain.valueobject.Email;
import com.paymentprocessor.userservice.domain.valueobject.PhoneNumber;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;

/**
 * User profile: the PII-bearing part of the {@code User} aggregate (email,
 * name, date of birth, optional phone) plus non-PII locale/timezone. All
 * invariants (name length, minimum age, valid email/phone) are enforced here in
 * the domain, not in controllers or persistence. Value objects (Email,
 * PhoneNumber) carry their own validation.
 */
@Getter
public class UserProfile {

    private static final int MIN_AGE_YEARS = 13;
    private static final int MAX_AGE_YEARS = 120;

    private Email email;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private PhoneNumber phone;   // optional
    private Locale locale;
    private ZoneId timezone;
    private Instant updatedAt;

    public UserProfile(Email email,
                       String firstName,
                       String lastName,
                       LocalDate dateOfBirth,
                       PhoneNumber phone,
                       Locale locale,
                       ZoneId timezone) {
        this.email = requireEmail(email);
        this.firstName = validateName(firstName, "firstName");
        this.lastName = validateName(lastName, "lastName");
        this.dateOfBirth = validateDob(dateOfBirth);
        this.phone = phone; // may be null
        this.locale = locale != null ? locale : Locale.ENGLISH;
        this.timezone = timezone != null ? timezone : ZoneId.of("UTC");
        this.updatedAt = DateTimeUtils.nowUtc();
    }

    public void changeEmail(Email email) {
        this.email = requireEmail(email);
        touch();
    }

    public void updateName(String firstName, String lastName) {
        this.firstName = validateName(firstName, "firstName");
        this.lastName = validateName(lastName, "lastName");
        touch();
    }

    public void updateDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = validateDob(dateOfBirth);
        touch();
    }

    public void updatePhone(PhoneNumber phone) {
        this.phone = phone;
        touch();
    }

    public void updateLocale(Locale locale) {
        this.locale = locale != null ? locale : Locale.ENGLISH;
        touch();
    }

    public void updateTimezone(ZoneId timezone) {
        this.timezone = timezone != null ? timezone : ZoneId.of("UTC");
        touch();
    }

    public Optional<PhoneNumber> phone() {
        return Optional.ofNullable(phone);
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    public int age() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    private void touch() {
        this.updatedAt = DateTimeUtils.nowUtc();
    }

    private static Email requireEmail(Email email) {
        if (email == null) {
            throw new ValidationException("email", "Email is required");
        }
        return email;
    }

    private static String validateName(String name, String field) {
        if (name == null || name.isBlank()) {
            throw new ValidationException(field, "Name must not be blank");
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2) {
            throw new ValidationException(field, "Name must be at least 2 characters");
        }
        if (trimmed.length() > 100) {
            throw new ValidationException(field, "Name must not exceed 100 characters");
        }
        return trimmed;
    }

    private static LocalDate validateDob(LocalDate dob) {
        if (dob == null) {
            throw new ValidationException("dateOfBirth", "Date of birth is required");
        }
        LocalDate today = LocalDate.now();
        if (dob.isAfter(today)) {
            throw new ValidationException("dateOfBirth", "Date of birth cannot be in the future");
        }
        int age = Period.between(dob, today).getYears();
        if (age < MIN_AGE_YEARS) {
            throw new ValidationException("dateOfBirth", "User must be at least " + MIN_AGE_YEARS + " years old");
        }
        if (age > MAX_AGE_YEARS) {
            throw new ValidationException("dateOfBirth", "Date of birth is not valid");
        }
        return dob;
    }
}
