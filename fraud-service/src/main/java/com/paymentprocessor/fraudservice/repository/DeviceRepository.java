package com.paymentprocessor.fraudservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.paymentprocessor.fraudservice.document.Device;

@Repository
public interface DeviceRepository extends MongoRepository<Device, String> {
}
