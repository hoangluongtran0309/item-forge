package com.hoangluongtran0309.domain.ability;

import com.hoangluongtran0309.domain.model.EffectCommand;
import com.hoangluongtran0309.domain.model.PotionEffectAbilityDefinition;

public class PotionEffectAbility implements Ability<PotionEffectAbilityDefinition, EffectCommand> {

    @Override
    public EffectCommand resolveEffect(PotionEffectAbilityDefinition definition) {
        return definition.toEffectCommand();
    }
}
