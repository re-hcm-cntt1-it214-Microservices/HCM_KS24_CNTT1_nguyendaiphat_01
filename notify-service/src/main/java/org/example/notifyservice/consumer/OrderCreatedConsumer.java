package org.example.notifyservice.consumer;

import org.example.notifyservice.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer {

    @Autowired
    private EmailService emailService;

    @KafkaListener(topics = "order-created-topic", groupId = "notify-group")
    public void consumeOrderCreated(Object orderNotification) {
        emailService.sendOrderNotification(orderNotification);
    }
}
