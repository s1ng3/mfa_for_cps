package com.cps.mfa.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {
    List<SecurityAlert> findTop50ByOrderByCreatedAtDesc();

    long countByReadStatusFalse();
}
