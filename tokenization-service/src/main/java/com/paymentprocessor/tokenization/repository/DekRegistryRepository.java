package com.paymentprocessor.tokenization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.paymentprocessor.tokenization.entity.DekRegistry;

@Repository
public interface DekRegistryRepository extends JpaRepository<DekRegistry, String> {
}
