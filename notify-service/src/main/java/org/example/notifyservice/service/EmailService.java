package org.example.notifyservice.service;

import org.springframework.stereotype.Service;

@Service
public class EmailService {

    public void sendOrderNotification(Object notificationData) {
        System.out.println("Processing email notification: " + notificationData);
    }
}
