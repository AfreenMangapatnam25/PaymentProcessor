package com.paymentprocessor.notificationservice.repository;

import com.paymentprocessor.notificationservice.entity.Suppression;
import com.paymentprocessor.notificationservice.entity.SuppressionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuppressionRepository extends JpaRepository<Suppression, SuppressionId> {

    boolean existsByIdChannelAndIdRecipientHash(String channel, byte[] recipientHash);
}
