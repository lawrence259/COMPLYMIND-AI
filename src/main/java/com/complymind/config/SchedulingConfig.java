package com.complymind.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring task scheduling only when app.scheduling.enabled=true (default).
 * Set app.scheduling.enabled=false in test configuration to prevent @Scheduled
 * tasks from running during integration tests.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
    // Marker configuration — scheduling is activated by this class presence.
}
