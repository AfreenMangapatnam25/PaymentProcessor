package com.paymentprocessor.fraudservice.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.paymentprocessor.fraudservice.document.Rule;

@Repository
public interface RuleRepository extends MongoRepository<Rule, String> {

    List<Rule> findByEnabledTrue();
}
