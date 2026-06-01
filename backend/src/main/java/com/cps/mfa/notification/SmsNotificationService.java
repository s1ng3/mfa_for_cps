package com.cps.mfa.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * MOCK SMS sender. Swap for Twilio/Vonage in production. Prints the message to the console.
 */
@Service
@Slf4j
public class SmsNotificationService {

    public void send(String phoneNumber, String message) {
        log.info("\n========== [MOCK SMS] ==========\nTo: {}\n{}\n================================",
                phoneNumber, message);
    }
}
