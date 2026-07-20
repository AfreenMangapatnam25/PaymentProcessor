package com.paymentprocessor.authenticationservice.service;

import com.paymentprocessor.authenticationservice.domain.DeviceTrust;
import com.paymentprocessor.authenticationservice.entity.Device;
import com.paymentprocessor.authenticationservice.exception.NotFoundException;
import com.paymentprocessor.authenticationservice.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DeviceService {

    private final DeviceRepository repository;

    public DeviceService(DeviceRepository repository) {
        this.repository = repository;
    }

    public record DeviceResolution(Device device, boolean isNew) {}

    /** Registers a device on first sight or updates last-seen metadata. */
    @Transactional
    public DeviceResolution registerOrTouch(String identityId, String fingerprint, String ip, String label) {
        if (fingerprint == null || fingerprint.isBlank()) {
            return new DeviceResolution(null, false);
        }
        return repository.findByIdentityIdAndFingerprint(identityId, fingerprint)
                .map(existing -> {
                    existing.setLastIp(ip);
                    existing.setLastSeenAt(Instant.now());
                    return new DeviceResolution(repository.save(existing), false);
                })
                .orElseGet(() -> {
                    Device device = new Device();
                    device.setId(UUID.randomUUID().toString());
                    device.setIdentityId(identityId);
                    device.setFingerprint(fingerprint);
                    device.setLabel(label);
                    device.setTrustLevel(DeviceTrust.UNKNOWN);
                    device.setLastIp(ip);
                    device.setLastSeenAt(Instant.now());
                    return new DeviceResolution(repository.save(device), true);
                });
    }

    @Transactional(readOnly = true)
    public List<Device> list(String identityId) {
        return repository.findByIdentityId(identityId);
    }

    @Transactional
    public void trust(String identityId, String deviceId) {
        Device device = repository.findByIdAndIdentityId(deviceId, identityId)
                .orElseThrow(() -> new NotFoundException("Device not found"));
        device.setTrustLevel(DeviceTrust.TRUSTED);
        repository.save(device);
    }

    @Transactional
    public void revoke(String identityId, String deviceId) {
        Device device = repository.findByIdAndIdentityId(deviceId, identityId)
                .orElseThrow(() -> new NotFoundException("Device not found"));
        repository.delete(device);
    }
}
