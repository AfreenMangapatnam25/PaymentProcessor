package com.paymentprocessor.disputeservice.repository;

import com.paymentprocessor.disputeservice.entity.ReasonCodeCatalog;
import com.paymentprocessor.disputeservice.entity.ReasonCodeCatalogId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReasonCodeCatalogRepository
        extends JpaRepository<ReasonCodeCatalog, ReasonCodeCatalogId> {

    List<ReasonCodeCatalog> findByNetwork(String network);
}
