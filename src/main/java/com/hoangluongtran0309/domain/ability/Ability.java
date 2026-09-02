package com.hoangluongtran0309.domain.ability;

import com.hoangluongtran0309.domain.model.AbilityDefinition;
import com.hoangluongtran0309.domain.model.AbilityEffect;

public interface Ability<D extends AbilityDefinition, E extends AbilityEffect> {

    E resolveEffect(D definition);
}
