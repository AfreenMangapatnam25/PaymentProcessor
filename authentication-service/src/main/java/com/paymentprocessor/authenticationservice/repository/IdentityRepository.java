package com.paymentprocessor.authenticationservice.repository;

import com.paymentprocessor.authenticationservice.entity.Identity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface IdentityRepository extends JpaRepository<Identity, String> {
    Optional<Identity> findByEmailIgnoreCase(String email);
    Optional<Identity> findByPhoneE164(String phoneE164);
    boolean existsByEmailIgnoreCase(String email);
}
