package com.cps.mfa.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * MOCK email sender. In a real deployment this would call an SMTP/Mailtrap/SES client.
 * For the demo it prints the message to the backend console so it is observable.
 */
@Service
@Slf4j
public class EmailNotificationService {

    public void send(String to, String subject, String body) {
        log.info("\n========== [MOCK EMAIL] ==========\nTo: {}\nSubject: {}\n{}\n==================================",
                to, subject, body);
    }
}
