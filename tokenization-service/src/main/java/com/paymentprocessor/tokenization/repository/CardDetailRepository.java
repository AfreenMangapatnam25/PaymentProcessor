package com.paymentprocessor.tokenization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.paymentprocessor.tokenization.entity.CardDetail;

@Repository
public interface CardDetailRepository extends JpaRepository<CardDetail, String> {
}
