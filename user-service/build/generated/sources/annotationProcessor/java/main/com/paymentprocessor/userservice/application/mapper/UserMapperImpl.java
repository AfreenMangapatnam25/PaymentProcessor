package com.paymentprocessor.userservice.application.mapper;

import com.paymentprocessor.userservice.api.response.UserProfileResponse;
import com.paymentprocessor.userservice.api.response.UserResponse;
import com.paymentprocessor.userservice.domain.user.User;
import com.paymentprocessor.userservice.domain.user.UserProfile;
import java.time.Instant;
import java.time.LocalDate;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-25T22:52:58+0530",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.jar, environment: Java 21.0.11 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserProfileResponse profile = null;
        long version = 0L;
        Instant createdAt = null;
        Instant updatedAt = null;
        Instant erasedAt = null;

        profile = toProfileResponse( user.getProfile() );
        version = user.getVersion();
        createdAt = user.getCreatedAt();
        updatedAt = user.getUpdatedAt();
        erasedAt = user.getErasedAt();

        String id = user.getId().value();
        String identityId = user.getIdentityId().value();
        String status = user.getStatus().name();

        UserResponse userResponse = new UserResponse( id, identityId, status, profile, version, createdAt, updatedAt, erasedAt );

        return userResponse;
    }

    @Override
    public UserProfileResponse toProfileResponse(UserProfile profile) {
        if ( profile == null ) {
            return null;
        }

        String firstName = null;
        String lastName = null;
        LocalDate dateOfBirth = null;

        firstName = profile.getFirstName();
        lastName = profile.getLastName();
        dateOfBirth = profile.getDateOfBirth();

        String email = profile.getEmail() != null ? profile.getEmail().value() : null;
        String phone = profile.phone().map(p -> p.value()).orElse(null);
        String locale = profile.getLocale() != null ? profile.getLocale().toLanguageTag() : null;
        String timezone = profile.getTimezone() != null ? profile.getTimezone().getId() : null;

        UserProfileResponse userProfileResponse = new UserProfileResponse( email, firstName, lastName, dateOfBirth, phone, locale, timezone );

        return userProfileResponse;
    }
}
