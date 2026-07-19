package com.paymentprocessor.fraudservice.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.paymentprocessor.fraudservice.document.ListEntry;

@Repository
public interface ListEntryRepository extends MongoRepository<ListEntry, String> {

    List<ListEntry> findByAttributeAndValue(String attribute, String value);
}
