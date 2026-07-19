package com.paymentprocessor.userservice.domain.address;

import com.paymentprocessor.userservice.common.exception.ValidationException;

/**
 * Polymorphic owner of an address: a type plus the owning aggregate's id. The
 * id's prefix must match the type (e.g. USER -> usr_), so a mismatched
 * type/id pair can never be constructed.
 */
public record AddressOwner(OwnerType type, String id) {

    public AddressOwner {
        if (type == null) {
            throw new ValidationException("ownerType", "Owner type is required");
        }
        if (id == null || id.isBlank()) {
            throw new ValidationException("ownerId", "Owner id is required");
        }
        if (!id.startsWith(type.idPrefix())) {
            throw new ValidationException("ownerId",
                    "Owner id must start with '" + type.idPrefix() + "' for owner type " + type);
        }
    }

    public static AddressOwner of(OwnerType type, String id) {
        return new AddressOwner(type, id);
    }
}
