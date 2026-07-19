package com.paymentprocessor.fraudservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.paymentprocessor.fraudservice.document.ModelRegistry;

@Repository
public interface ModelRegistryRepository extends MongoRepository<ModelRegistry, String> {
}
