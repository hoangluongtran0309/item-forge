package com.hoangluongtran0309.infrastructure.bukkit.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import com.hoangluongtran0309.domain.ArmorRegistry;
import com.hoangluongtran0309.domain.ItemRegistry;
import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.domain.model.ShapedRecipeDefinition;
import com.hoangluongtran0309.domain.model.ShapelessRecipeDefinition;
import com.hoangluongtran0309.infrastructure.bukkit.ArmorStackFactory;
import com.hoangluongtran0309.infrastructure.bukkit.ItemStackFactory;
import com.hoangluongtran0309.infrastructure.bukkit.model.ArmorModelStrategy;
import com.hoangluongtran0309.infrastructure.bukkit.model.ItemModelStrategy;

class RecipeRegistrarTest {

    private ServerMock server;
    private PluginMock plugin;
    private RecipeRegistrar registrar;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("ItemForge");

        ItemRegistry itemRegistry = new ItemRegistry();
        ArmorRegistry armorRegistry = new ArmorRegistry();
        ItemStackFactory itemStackFactory = new ItemStackFactory(plugin, new NoopItemModelStrategy());
        ArmorStackFactory armorStackFactory = new ArmorStackFactory(plugin, new NoopArmorModelStrategy());
        RecipeIngredientResolver resolver = new RecipeIngredientResolver(itemRegistry, armorRegistry,
                itemStackFactory, armorStackFactory);

        registrar = new RecipeRegistrar(plugin, resolver, Logger.getAnonymousLogger());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void registersShapedRecipeRetrievableFromServer() {
        ShapedRecipeDefinition definition = new ShapedRecipeDefinition("void_sword", "NETHERITE_SWORD", 1,
                List.of("B"), Map.of('B', "BLAZE_POWDER"));

        registrar.registerAll(List.of(definition));

        NamespacedKey key = new NamespacedKey(plugin, "void_sword");
        assertNotNull(server.getRecipe(key));
    }

    @Test
    void registersShapelessRecipeRetrievableFromServer() {
        ShapelessRecipeDefinition definition = new ShapelessRecipeDefinition("void_sword_salvage", "TORCH", 4,
                List.of("COAL", "STICK"));

        registrar.registerAll(List.of(definition));

        NamespacedKey key = new NamespacedKey(plugin, "void_sword_salvage");
        assertNotNull(server.getRecipe(key));
    }

    @Test
    void unregisterAllRemovesTrackedRecipes() {
        ShapelessRecipeDefinition definition = new ShapelessRecipeDefinition("void_sword_salvage", "TORCH", 4,
                List.of("COAL", "STICK"));
        registrar.registerAll(List.of(definition));

        registrar.unregisterAll();

        NamespacedKey key = new NamespacedKey(plugin, "void_sword_salvage");
        assertNull(server.getRecipe(key));
    }

    @Test
    void reloadCanBeCalledRepeatedlyWithoutThrowing() {
        ShapelessRecipeDefinition definition = new ShapelessRecipeDefinition("void_sword_salvage", "TORCH", 4,
                List.of("COAL", "STICK"));

        registrar.registerAll(List.of(definition));

        assertDoesNotThrow(() -> registrar.reload(List.of(definition)));
        assertDoesNotThrow(() -> registrar.reload(List.of(definition)));

        NamespacedKey key = new NamespacedKey(plugin, "void_sword_salvage");
        assertNotNull(server.getRecipe(key));
    }

    @Test
    void unresolvableRecipeIsSkippedWithoutBlockingOthers() {
        ShapelessRecipeDefinition broken = new ShapelessRecipeDefinition("broken", "not_a_real_id", 1,
                List.of("COAL"));
        ShapelessRecipeDefinition fine = new ShapelessRecipeDefinition("void_sword_salvage", "TORCH", 4,
                List.of("COAL", "STICK"));

        registrar.registerAll(List.of(broken, fine));

        assertNull(server.getRecipe(new NamespacedKey(plugin, "broken")));
        assertNotNull(server.getRecipe(new NamespacedKey(plugin, "void_sword_salvage")));
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
