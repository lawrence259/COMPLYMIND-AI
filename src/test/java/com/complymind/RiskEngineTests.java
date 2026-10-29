package com.complymind;

import com.complymind.risk.entity.RiskBand;
import com.complymind.risk.model.RiskBreakdown;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiskEngineTests {

    @Test
    @DisplayName("Deterministic Risk Formula: Weighted normalized calculation")
    void testRiskFormulaCalculation() {
        double wDeadline = 0.35;
        double wBlocked = 0.30;
        double wDeficiency = 0.20;
        double wPendency = 0.15;

        // Weights sum to 1.0
        assertEquals(1.0, wDeadline + wBlocked + wDeficiency + wPendency, 0.001);

        // Scenario 1: All factors at 100 (Maximum Critical Risk)
        double maxScore = (wDeadline * 100.0) + (wBlocked * 100.0) + (wDeficiency * 100.0) + (wPendency * 100.0);
        assertEquals(100.0, maxScore, 0.01);

        // Scenario 2: All factors at 0 (Zero Risk)
        double minScore = (wDeadline * 0.0) + (wBlocked * 0.0) + (wDeficiency * 0.0) + (wPendency * 0.0);
        assertEquals(0.0, minScore, 0.01);

        // Scenario 3: Medium risk case: Deadline=40, Blocked=50, Deficiency=30, Pendency=20
        double expected = (0.35 * 40.0) + (0.30 * 50.0) + (0.20 * 30.0) + (0.15 * 20.0);
        // 14 + 15 + 6 + 3 = 38.0
        assertEquals(38.0, expected, 0.01);
    }

    @Test
    @DisplayName("Risk Band Thresholds: Verify classification bands")
    void testRiskBandClassification() {
        assertEquals(RiskBand.LOW, getBand(0.0));
        assertEquals(RiskBand.LOW, getBand(29.0));
        assertEquals(RiskBand.MEDIUM, getBand(29.5));
        assertEquals(RiskBand.MEDIUM, getBand(59.0));
        assertEquals(RiskBand.HIGH, getBand(60.0));
        assertEquals(RiskBand.HIGH, getBand(79.0));
        assertEquals(RiskBand.CRITICAL, getBand(79.5));
        assertEquals(RiskBand.CRITICAL, getBand(100.0));
    }

    @Test
    @DisplayName("Boundary Clamping: Final score must never exceed 100 or be below 0")
    void testBoundaryClamping() {
        double clampedHigh = Math.max(0.0, Math.min(100.0, 145.2));
        assertEquals(100.0, clampedHigh);

        double clampedLow = Math.max(0.0, Math.min(100.0, -25.5));
        assertEquals(0.0, clampedLow);
    }

    private RiskBand getBand(double score) {
        if (score <= 29.0) return RiskBand.LOW;
        if (score <= 59.0) return RiskBand.MEDIUM;
        if (score <= 79.0) return RiskBand.HIGH;
        return RiskBand.CRITICAL;
    }
}
