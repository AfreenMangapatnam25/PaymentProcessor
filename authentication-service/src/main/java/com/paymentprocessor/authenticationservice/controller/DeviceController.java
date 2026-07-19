package com.paymentprocessor.authenticationservice.controller;

import com.paymentprocessor.authenticationservice.dto.DeviceResponse;
import com.paymentprocessor.authenticationservice.security.AuthPrincipal;
import com.paymentprocessor.authenticationservice.service.DeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public List<DeviceResponse> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return deviceService.list(principal.identityId()).stream().map(DeviceResponse::from).toList();
    }

    @PostMapping("/{deviceId}/trust")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trust(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable String deviceId) {
        deviceService.trust(principal.identityId(), deviceId);
    }

    @DeleteMapping("/{deviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable String deviceId) {
        deviceService.revoke(principal.identityId(), deviceId);
    }
}
