package com.paymentprocessor.userservice.application.mapper;

import com.paymentprocessor.userservice.api.response.UserProfileResponse;
import com.paymentprocessor.userservice.api.response.UserResponse;
import com.paymentprocessor.userservice.domain.user.User;
import com.paymentprocessor.userservice.domain.user.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps the User aggregate to API response DTOs. MapStruct generates the
 * implementation (Spring component). Value objects are unwrapped via
 * expressions; PII is included here because responses go only to authorized
 * callers -- it must still never be logged (rule 13).
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", expression = "java(user.getId().value())")
    @Mapping(target = "identityId", expression = "java(user.getIdentityId().value())")
    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    @Mapping(target = "profile", source = "profile")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "erasedAt", source = "erasedAt")
    UserResponse toResponse(User user);

    @Mapping(target = "email", expression = "java(profile.getEmail() != null ? profile.getEmail().value() : null)")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "dateOfBirth", source = "dateOfBirth")
    @Mapping(target = "phone", expression = "java(profile.phone().map(p -> p.value()).orElse(null))")
    @Mapping(target = "locale", expression = "java(profile.getLocale() != null ? profile.getLocale().toLanguageTag() : null)")
    @Mapping(target = "timezone", expression = "java(profile.getTimezone() != null ? profile.getTimezone().getId() : null)")
    UserProfileResponse toProfileResponse(UserProfile profile);
}
