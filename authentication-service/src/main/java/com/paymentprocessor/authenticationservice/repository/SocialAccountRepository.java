package com.paymentprocessor.authenticationservice.repository;

import com.paymentprocessor.authenticationservice.domain.SocialProvider;
import com.paymentprocessor.authenticationservice.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence access for {@link SocialAccount} link rows.
 *
 * <p>Used by
 * {@link com.paymentprocessor.authenticationservice.service.SocialLoginService} to decide,
 * on each social login, whether this is a returning user (link row exists), an existing
 * local user adopting a new provider (link row absent but email matches an identity), or
 * a brand-new user (neither).
 */
@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, String> {

    /**
     * Primary lookup for a returning social login. Matches on the provider's immutable
     * subject id rather than email, so a user changing their email at the provider still
     * resolves to the same local identity.
     *
     * @param provider       the external provider
     * @param providerUserId the provider-assigned immutable user id (OIDC {@code sub})
     * @return the existing link row, if this external account has logged in before
     */
    Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    /**
     * Lists every external account linked to a local identity — used to render
     * "connected accounts" in account settings and to prevent a user unlinking their
     * only remaining login method.
     *
     * @param identityId the local identity id
     * @return all link rows for that identity (possibly empty)
     */
    List<SocialAccount> findByIdentityId(String identityId);

    /**
     * @param provider       the external provider
     * @param providerUserId the provider-assigned user id
     * @return true if this external account is already linked to some local identity
     */
    boolean existsByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}
