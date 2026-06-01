package com.cps.mfa.risk;

/** A single contributing factor to a risk score (rule name + points added). */
public record RiskReason(String rule, int points) {
}
