package com.hoangluongtran0309.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownService {

    private final Map<CooldownKey, Instant> expiryByKey = new ConcurrentHashMap<>();

    public void markUsed(UUID playerId, String abilityKey, Duration cooldown) {
        expiryByKey.put(new CooldownKey(playerId, abilityKey), Instant.now().plus(cooldown));
    }

    public boolean isOnCooldown(UUID playerId, String abilityKey) {
        CooldownKey key = new CooldownKey(playerId, abilityKey);
        Instant expiry = expiryByKey.get(key);
        if (expiry == null) {
            return false;
        }

        if (Instant.now().isBefore(expiry)) {
            return true;
        }

        // Clean up as soon as it expires, so the map cannot grow without bound.
        expiryByKey.remove(key);
        return false;
    }

    private record CooldownKey(UUID playerId, String abilityKey) {
    }
}
