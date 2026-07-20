package com.paymentprocessor.authorization.dto.access;

import java.util.Set;

public record EffectivePermissionsResponse(String identityId, Set<String> permissions) {
}
