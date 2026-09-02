package com.hoangluongtran0309.itemforge.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PotionEffectAbilityJson.class, name = "POTION_EFFECT"),
        @JsonSubTypes.Type(value = DamageBonusAbilityJson.class, name = "DAMAGE_BONUS")
})
public sealed interface AbilityJson permits PotionEffectAbilityJson, DamageBonusAbilityJson {

    String type();

    String trigger();
}
