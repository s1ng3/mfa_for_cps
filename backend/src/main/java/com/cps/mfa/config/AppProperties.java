package com.cps.mfa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Strongly-typed binding for the {@code app.*} configuration tree in application.yml.
 * Centralising these values keeps timeout/OTP policy out of the business code.
 */
@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private Cors cors = new Cors();
    private Session session = new Session();
    private Otp otp = new Otp();
    private Recovery recovery = new Recovery();
    private Mfa mfa = new Mfa();
    private WorkingHours workingHours = new WorkingHours();

    @Data
    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";
    }

    @Data
    public static class Session {
        private int idleTimeoutMinutes = 5;
        private int absoluteTimeoutMinutes = 30;
        private int adminTimeoutMinutes = 15;
        private int stepUpValiditySeconds = 120;
    }

    @Data
    public static class Otp {
        private int length = 6;
        private int ttlMinutes = 5;
        private int maxAttempts = 3;
        private int resendCooldownSeconds = 30;
    }

    @Data
    public static class Recovery {
        private int codeCount = 10;
    }

    @Data
    public static class Mfa {
        private boolean mockPrintOtp = true;
    }

    @Data
    public static class WorkingHours {
        private int start = 7;
        private int end = 19;
    }
}
