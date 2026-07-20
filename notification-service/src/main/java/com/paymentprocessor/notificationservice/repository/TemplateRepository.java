package com.paymentprocessor.notificationservice.repository;

import com.paymentprocessor.notificationservice.entity.Template;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateRepository extends JpaRepository<Template, String> {

    Optional<Template> findTopByKeyAndChannelAndLocaleOrderByVersionDesc(String key, String channel, String locale);

    Optional<Template> findByKeyAndChannelAndLocaleAndVersion(String key, String channel, String locale, Integer version);

    List<Template> findByKeyAndChannel(String key, String channel);
}
