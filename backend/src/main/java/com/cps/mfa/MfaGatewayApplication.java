package com.cps.mfa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Adaptive MFA Security Gateway.
 *
 * <p>The gateway sits in front of a simulated HMI/SCADA workstation. Operators must pass
 * authentication + risk-based MFA before any process value or control action is reachable.</p>
 */
@SpringBootApplication
@EnableScheduling // SessionMonitor uses scheduled tasks to expire idle/absolute-timed sessions
public class MfaGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MfaGatewayApplication.class, args);
    }
}
