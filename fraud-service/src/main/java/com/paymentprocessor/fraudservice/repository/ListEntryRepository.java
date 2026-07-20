package com.paymentprocessor.fraudservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.paymentprocessor.fraudservice.domain.entity.ListEntry;

@Repository
public interface ListEntryRepository extends JpaRepository<ListEntry, UUID> {

    List<ListEntry> findByAttributeAndValue(String attribute, String value);
}
