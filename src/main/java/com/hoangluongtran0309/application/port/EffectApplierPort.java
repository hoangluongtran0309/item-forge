package com.hoangluongtran0309.application.port;

import java.util.UUID;

import com.hoangluongtran0309.domain.model.AbilityEffect;

public interface EffectApplierPort {

    void apply(UUID playerId, AbilityEffect effect);
}
