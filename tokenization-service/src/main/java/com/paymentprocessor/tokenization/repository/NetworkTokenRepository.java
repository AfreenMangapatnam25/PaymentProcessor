package com.paymentprocessor.tokenization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.paymentprocessor.tokenization.entity.NetworkToken;

@Repository
public interface NetworkTokenRepository extends JpaRepository<NetworkToken, String> {
}
