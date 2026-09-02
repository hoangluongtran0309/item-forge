package com.hoangluongtran0309.infrastructure.bukkit.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import com.hoangluongtran0309.domain.ArmorRegistry;
import com.hoangluongtran0309.domain.ItemRegistry;
import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.ArmorSlot;
import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.infrastructure.bukkit.ArmorStackFactory;
import com.hoangluongtran0309.infrastructure.bukkit.ItemStackFactory;
import com.hoangluongtran0309.infrastructure.bukkit.model.ArmorModelStrategy;
import com.hoangluongtran0309.infrastructure.bukkit.model.ItemModelStrategy;

class RecipeIngredientResolverTest {

    private PluginMock plugin;
    private ItemRegistry itemRegistry;
    private ArmorRegistry armorRegistry;
    private RecipeIngredientResolver resolver;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("ItemForge");

        itemRegistry = new ItemRegistry();
        armorRegistry = new ArmorRegistry();
        ItemStackFactory itemStackFactory = new ItemStackFactory(plugin, new NoopItemModelStrategy());
        ArmorStackFactory armorStackFactory = new ArmorStackFactory(plugin, new NoopArmorModelStrategy());

        resolver = new RecipeIngredientResolver(itemRegistry, armorRegistry, itemStackFactory, armorStackFactory);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void resolvesVanillaMaterialChoiceAndStack() {
        Optional<RecipeChoice> choice = resolver.resolveChoice("DIAMOND");
        assertInstanceOf(RecipeChoice.MaterialChoice.class, choice.orElseThrow());

        ItemStack stack = resolver.resolveItemStack("DIAMOND", 3).orElseThrow();
        assertEquals(Material.DIAMOND, stack.getType());
        assertEquals(3, stack.getAmount());
    }

    @Test
    void resolvesCustomItemViaItemRegistry() {
        itemRegistry.register(new ItemDefinition("void_sword", "NETHERITE_SWORD", 1001, "Fire Sword", List.of(),
                List.of()));

        Optional<RecipeChoice> choice = resolver.resolveChoice("void_sword");
        assertInstanceOf(RecipeChoice.ExactChoice.class, choice.orElseThrow());

        ItemStack stack = resolver.resolveItemStack("void_sword", 2).orElseThrow();
        assertEquals(Material.NETHERITE_SWORD, stack.getType());
        assertEquals(2, stack.getAmount());
    }

    @Test
    void resolvesCustomArmorViaArmorRegistry() {
        armorRegistry.register(
                new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 0, "Dragon Helm",
                        List.of()));

        Optional<RecipeChoice> choice = resolver.resolveChoice("void_helmet");
        assertInstanceOf(RecipeChoice.ExactChoice.class, choice.orElseThrow());

        ItemStack stack = resolver.resolveItemStack("void_helmet", 1).orElseThrow();
        assertEquals(Material.NETHERITE_HELMET, stack.getType());
    }

    @Test
    void unknownIdResolvesToEmpty() {
        assertTrue(resolver.resolveChoice("not_a_real_id").isEmpty());
        assertTrue(resolver.resolveItemStack("not_a_real_id", 1).isEmpty());
    }

    private static final class NoopItemModelStrategy implements ItemModelStrategy {
        @Override
        public void applyModel(ItemMeta meta, ItemDefinition definition) {
        }

        @Override
        public void generateResourcePackFiles(List<ItemDefinition> items, Path textureSourceDir, String namespace,
                Path outputDir) throws IOException {
        }
    }

    private static final class NoopArmorModelStrategy implements ArmorModelStrategy {
        @Override
        public void applyModel(ItemMeta meta, ArmorDefinition definition) {
        }

        @Override
        public void generateResourcePackFiles(List<ArmorDefinition> armors, Path textureSourceDir, String namespace,
                Path outputDir) throws IOException {
        }
    }
}
