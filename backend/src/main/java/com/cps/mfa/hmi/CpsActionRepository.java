package com.cps.mfa.hmi;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CpsActionRepository extends JpaRepository<CpsAction, Long> {
    List<CpsAction> findTop50ByOrderByCreatedAtDesc();
}
