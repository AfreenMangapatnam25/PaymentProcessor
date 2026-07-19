package com.paymentprocessor.fraudservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.paymentprocessor.fraudservice.domain.entity.Rule;

@Repository
public interface RuleRepository extends JpaRepository<Rule, UUID> {

    List<Rule> findByEnabledTrue();
}
