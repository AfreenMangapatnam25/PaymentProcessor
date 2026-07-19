package com.paymentprocessor.authenticationservice.repository;

import com.paymentprocessor.authenticationservice.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    Optional<Device> findByIdentityIdAndFingerprint(String identityId, String fingerprint);
    List<Device> findByIdentityId(String identityId);
    Optional<Device> findByIdAndIdentityId(String id, String identityId);
}
