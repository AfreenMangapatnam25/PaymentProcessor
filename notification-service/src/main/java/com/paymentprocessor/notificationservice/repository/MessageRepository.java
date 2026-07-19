package com.paymentprocessor.notificationservice.repository;

import com.paymentprocessor.notificationservice.entity.Message;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    List<Message> findByChannelAndRecipientHash(String channel, byte[] recipientHash, Pageable pageable);
}
