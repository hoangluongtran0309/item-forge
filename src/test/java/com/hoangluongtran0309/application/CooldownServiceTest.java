package com.hoangluongtran0309.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CooldownServiceTest {

    private final CooldownService service = new CooldownService();

    @Test
    void notOnCooldownBeforeAnyUse() {
        assertFalse(service.isOnCooldown(UUID.randomUUID(), "sword#0"));
    }

    @Test
    void onCooldownRightAfterMarkUsed() {
        UUID playerId = UUID.randomUUID();
        service.markUsed(playerId, "sword#0", Duration.ofSeconds(60));

        assertTrue(service.isOnCooldown(playerId, "sword#0"));
    }

    @Test
    void noLongerOnCooldownAfterExpiry() {
        UUID playerId = UUID.randomUUID();
        // A negative duration expires immediately, which avoids Thread.sleep in a test.
        service.markUsed(playerId, "sword#0", Duration.ofSeconds(-1));

        assertFalse(service.isOnCooldown(playerId, "sword#0"));
    }

    @Test
    void cooldownIsIndependentPerPlayer() {
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();
        service.markUsed(playerA, "sword#0", Duration.ofSeconds(60));

        assertTrue(service.isOnCooldown(playerA, "sword#0"));
        assertFalse(service.isOnCooldown(playerB, "sword#0"));
    }

    @Test
    void cooldownIsIndependentPerAbilityKey() {
        UUID playerId = UUID.randomUUID();
        service.markUsed(playerId, "sword#0", Duration.ofSeconds(60));

        assertTrue(service.isOnCooldown(playerId, "sword#0"));
        assertFalse(service.isOnCooldown(playerId, "sword#1"));
    }
}
