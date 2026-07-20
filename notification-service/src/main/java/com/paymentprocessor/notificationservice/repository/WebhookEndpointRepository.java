package com.paymentprocessor.notificationservice.repository;

import com.paymentprocessor.notificationservice.entity.WebhookEndpoint;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, String> {

    List<WebhookEndpoint> findByMerchantId(String merchantId);

    @Query(value = "SELECT * FROM webhook_endpoints "
            + "WHERE merchant_id = :merchantId AND status = 'active' AND :eventType = ANY(subscribed_types)",
            nativeQuery = true)
    List<WebhookEndpoint> findActiveSubscribers(@Param("merchantId") String merchantId,
                                                 @Param("eventType") String eventType);
}
