package com.paymentprocessor.fraudservice.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.paymentprocessor.fraudservice.domain.entity.ModelRegistry;

@Repository
public interface ModelRegistryRepository extends JpaRepository<ModelRegistry, UUID> {
}
