package com.hoangluongtran0309.itemforge.dashboard.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.itemforge.dashboard.dto.AbilityJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.DamageBonusAbilityJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.ItemJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.PotionEffectAbilityJson;
import com.hoangluongtran0309.itemforge.dashboard.form.AbilityFormRow;
import com.hoangluongtran0309.itemforge.dashboard.form.ItemForm;

class ItemFormMapperTest {

    private final ItemFormMapper mapper = new ItemFormMapper();

    @Test
    void toJsonBuildsAPotionEffectAbility() {
        ItemForm form = new ItemForm();
        form.setId("void_sword");
        form.setMaterial("NETHERITE_SWORD");
        form.setDisplayName("Fire Sword");
        form.setLoreText("A blazing blade.\nHandle with care.");

        AbilityFormRow row = new AbilityFormRow();
        row.setType("POTION_EFFECT");
        row.setTrigger("RIGHT_CLICK");
        row.setCooldownSeconds(60);
        row.setEffect("FIRE_RESISTANCE");
        row.setDurationSeconds(30);
        form.setAbilities(List.of(row));

        ItemJson json = mapper.toJson(form);

        assertEquals("void_sword", json.id());
        assertEquals("NETHERITE_SWORD", json.material());
        assertEquals(List.of("A blazing blade.", "Handle with care."), json.lore());
        assertEquals(1, json.abilities().size());
        PotionEffectAbilityJson ability = assertInstanceOf(PotionEffectAbilityJson.class, json.abilities().get(0));
        assertEquals("FIRE_RESISTANCE", ability.effect());
        assertEquals(30, ability.durationSeconds());
    }

    @Test
    void toJsonBuildsADamageBonusAbility() {
        ItemForm form = new ItemForm();
        form.setId("void_pickaxe");
        form.setMaterial("NETHERITE_PICKAXE");
        form.setDisplayName("Power Axe");
        form.setLoreText("");

        AbilityFormRow row = new AbilityFormRow();
        row.setType("DAMAGE_BONUS");
        row.setTrigger("ON_HIT");
        row.setCooldownSeconds(10);
        row.setBonusPercent(15.5);
        form.setAbilities(List.of(row));

        ItemJson json = mapper.toJson(form);

        DamageBonusAbilityJson ability = assertInstanceOf(DamageBonusAbilityJson.class, json.abilities().get(0));
        assertEquals(15.5, ability.bonusPercent());
    }

    @Test
    void toJsonSkipsIncompleteAbilityRows() {
        ItemForm form = new ItemForm();
        form.setId("plain_item");
        form.setMaterial("STICK");
        form.setDisplayName("Plain");
        form.setLoreText("");

        AbilityFormRow blankRow = new AbilityFormRow();
        blankRow.setType("");
        AbilityFormRow incompletePotion = new AbilityFormRow();
        incompletePotion.setType("POTION_EFFECT");
        incompletePotion.setTrigger("RIGHT_CLICK");
        incompletePotion.setCooldownSeconds(5);
        // effect/durationSeconds are left null on purpose.
        form.setAbilities(List.of(blankRow, incompletePotion));

        ItemJson json = mapper.toJson(form);

        assertTrue(json.abilities().isEmpty());
    }

    @Test
    void toJsonDefaultsCustomModelDataToZeroWhenUnset() {
        ItemForm form = new ItemForm();
        form.setId("new_item");
        form.setMaterial("STICK");
        form.setDisplayName("New Item");
        form.setLoreText("");
        form.setAbilities(List.of());

        ItemJson json = mapper.toJson(form);

        assertEquals(0, json.customModelData());
    }

    @Test
    void fromJsonThenToJsonRoundTrips() {
        ItemJson original = new ItemJson("void_sword", "NETHERITE_SWORD", 1001, "Fire Sword",
                List.of("A blazing blade."),
                List.<AbilityJson>of(new PotionEffectAbilityJson("POTION_EFFECT", "RIGHT_CLICK", 60,
                        "FIRE_RESISTANCE", 30)));

        ItemForm form = mapper.fromJson(original);
        ItemJson roundTripped = mapper.toJson(form);

        assertEquals(original, roundTripped);
    }
}
