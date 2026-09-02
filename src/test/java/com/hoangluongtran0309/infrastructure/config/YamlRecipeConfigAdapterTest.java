package com.hoangluongtran0309.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.hoangluongtran0309.domain.model.RecipeDefinition;
import com.hoangluongtran0309.domain.model.ShapedRecipeDefinition;
import com.hoangluongtran0309.domain.model.ShapelessRecipeDefinition;

class YamlRecipeConfigAdapterTest {

    @Test
    void missingFileReturnsEmptyList(@TempDir Path dir) {
        YamlRecipeConfigAdapter adapter = new YamlRecipeConfigAdapter(dir.resolve("recipes.yml"),
                Logger.getAnonymousLogger());
        assertTrue(adapter.loadAll().isEmpty());
    }

    @Test
    void shapedRecipeParsesCorrectly(@TempDir Path dir) throws IOException {
        Path recipesFile = dir.resolve("recipes.yml");
        Files.writeString(recipesFile, """
                recipes:
                  void_sword:
                    type: SHAPED
                    result: void_sword
                    result-count: 1
                    shape:
                      - " B "
                      - " S "
                      - " I "
                    ingredients:
                      B: BLAZE_POWDER
                      S: NETHERITE_SWORD
                      I: STICK
                """);

        YamlRecipeConfigAdapter adapter = new YamlRecipeConfigAdapter(recipesFile, Logger.getAnonymousLogger());
        List<RecipeDefinition> result = adapter.loadAll();

        assertEquals(1, result.size());
        ShapedRecipeDefinition parsed = assertInstanceOf(ShapedRecipeDefinition.class, result.get(0));
        assertEquals("void_sword", parsed.id());
        assertEquals("void_sword", parsed.resultId());
        assertEquals(1, parsed.resultCount());
        assertEquals(List.of(" B ", " S ", " I "), parsed.shape());
        assertEquals("BLAZE_POWDER", parsed.ingredients().get('B'));
        assertEquals("NETHERITE_SWORD", parsed.ingredients().get('S'));
        assertEquals("STICK", parsed.ingredients().get('I'));
    }

    @Test
    void shapelessRecipeParsesCorrectlyIncludingRepeats(@TempDir Path dir) throws IOException {
        Path recipesFile = dir.resolve("recipes.yml");
        Files.writeString(recipesFile, """
                recipes:
                  void_sword_salvage:
                    type: SHAPELESS
                    result: TORCH
                    result-count: 4
                    ingredients:
                      - COAL
                      - COAL
                      - STICK
                      - STICK
                """);

        YamlRecipeConfigAdapter adapter = new YamlRecipeConfigAdapter(recipesFile, Logger.getAnonymousLogger());
        List<RecipeDefinition> result = adapter.loadAll();

        assertEquals(1, result.size());
        ShapelessRecipeDefinition parsed = assertInstanceOf(ShapelessRecipeDefinition.class, result.get(0));
        assertEquals("TORCH", parsed.resultId());
        assertEquals(4, parsed.resultCount());
        assertEquals(List.of("COAL", "COAL", "STICK", "STICK"), parsed.ingredientIds());
    }

    @Test
    void missingResultCountDefaultsToOne(@TempDir Path dir) throws IOException {
        Path recipesFile = dir.resolve("recipes.yml");
        Files.writeString(recipesFile, """
                recipes:
                  void_sword_salvage:
                    type: SHAPELESS
                    result: TORCH
                    ingredients:
                      - COAL
                """);

        YamlRecipeConfigAdapter adapter = new YamlRecipeConfigAdapter(recipesFile, Logger.getAnonymousLogger());
        List<RecipeDefinition> result = adapter.loadAll();

        assertEquals(1, result.get(0).resultCount());
    }

    @Test
    void unknownRecipeTypeSkipsOnlyThatEntry(@TempDir Path dir) throws IOException {
        Path recipesFile = dir.resolve("recipes.yml");
        Files.writeString(recipesFile, """
                recipes:
                  broken_recipe:
                    type: NOT_A_REAL_TYPE
                    result: TORCH
                  fine_recipe:
                    type: SHAPELESS
                    result: TORCH
                    ingredients:
                      - COAL
                """);

        YamlRecipeConfigAdapter adapter = new YamlRecipeConfigAdapter(recipesFile, Logger.getAnonymousLogger());
        List<RecipeDefinition> result = adapter.loadAll();

        assertEquals(1, result.size());
        assertEquals("fine_recipe", result.get(0).id());
    }

    @Test
    void missingResultSkipsEntryWithoutCrashing(@TempDir Path dir) throws IOException {
        Path recipesFile = dir.resolve("recipes.yml");
        Files.writeString(recipesFile, """
                recipes:
                  broken_recipe:
                    type: SHAPELESS
                    ingredients:
                      - COAL
                """);

        YamlRecipeConfigAdapter adapter = new YamlRecipeConfigAdapter(recipesFile, Logger.getAnonymousLogger());
        assertTrue(adapter.loadAll().isEmpty());
    }

    @Test
    void malformedYamlSyntaxReturnsEmptyListWithoutCrashing(@TempDir Path dir) throws IOException {
        Path recipesFile = dir.resolve("recipes.yml");
        Files.writeString(recipesFile, """
                recipes:
                  broken_recipe: [unclosed
                """);

        YamlRecipeConfigAdapter adapter = new YamlRecipeConfigAdapter(recipesFile, Logger.getAnonymousLogger());

        assertTrue(adapter.loadAll().isEmpty());
    }

    @Test
    void multiCharacterIngredientKeySkipsEntryWithoutCrashingFile(@TempDir Path dir) throws IOException {
        Path recipesFile = dir.resolve("recipes.yml");
        Files.writeString(recipesFile, """
                recipes:
                  broken_recipe:
                    type: SHAPED
                    result: void_sword
                    shape:
                      - "AA"
                    ingredients:
                      AA: DIAMOND
                  fine_recipe:
                    type: SHAPELESS
                    result: TORCH
                    ingredients:
                      - COAL
                """);

        YamlRecipeConfigAdapter adapter = new YamlRecipeConfigAdapter(recipesFile, Logger.getAnonymousLogger());
        List<RecipeDefinition> result = adapter.loadAll();

        assertEquals(1, result.size());
        assertEquals("fine_recipe", result.get(0).id());
    }

    @Test
    void saveThenLoadAllRoundTripsANewShapedRecipe(@TempDir Path dir) {
        Path recipesFile = dir.resolve("recipes.yml");
        YamlRecipeConfigAdapter adapter = new YamlRecipeConfigAdapter(recipesFile, Logger.getAnonymousLogger());

        RecipeDefinition definition = new ShapedRecipeDefinition("void_sword", "void_sword", 1,
                List.of(" B ", " S ", " I "), Map.of('B', "BLAZE_POWDER", 'S', "NETHERITE_SWORD", 'I', "STICK"));

        adapter.save(definition);
        List<RecipeDefinition> result = adapter.loadAll();

        assertEquals(1, result.size());
        assertEquals(definition, result.get(0));
    }

    @Test
    void saveAddsARecipeWithoutRemovingExistingOnes(@TempDir Path dir) throws IOException {
        Path recipesFile = dir.resolve("recipes.yml");
        Files.writeString(recipesFile, """
                recipes:
                  void_sword_salvage:
                    type: SHAPELESS
                    result: TORCH
                    ingredients:
                      - COAL
                """);

        YamlRecipeConfigAdapter adapter = new YamlRecipeConfigAdapter(recipesFile, Logger.getAnonymousLogger());
        adapter.save(new ShapelessRecipeDefinition("void_sword_bundle", "void_sword", 1, List.of("STICK")));

        List<RecipeDefinition> result = adapter.loadAll();
        assertEquals(2, result.size());
    }

    @Test
    void saveOverwritesAnExistingEntryWithTheSameId(@TempDir Path dir) {
        Path recipesFile = dir.resolve("recipes.yml");
        YamlRecipeConfigAdapter adapter = new YamlRecipeConfigAdapter(recipesFile, Logger.getAnonymousLogger());

        adapter.save(new ShapelessRecipeDefinition("void_sword_salvage", "TORCH", 4, List.of("COAL")));
        adapter.save(new ShapelessRecipeDefinition("void_sword_salvage", "TORCH", 8, List.of("COAL", "COAL")));

        List<RecipeDefinition> result = adapter.loadAll();
        assertEquals(1, result.size());
        assertEquals(8, result.get(0).resultCount());
    }

    @Test
    void deleteRemovesOnlyTheMatchingEntry(@TempDir Path dir) {
        Path recipesFile = dir.resolve("recipes.yml");
        YamlRecipeConfigAdapter adapter = new YamlRecipeConfigAdapter(recipesFile, Logger.getAnonymousLogger());

        adapter.save(new ShapelessRecipeDefinition("void_sword_salvage", "TORCH", 4, List.of("COAL")));
        adapter.save(new ShapelessRecipeDefinition("stick_bundle", "STICK", 4, List.of("PLANKS")));

        adapter.delete("void_sword_salvage");

        List<RecipeDefinition> result = adapter.loadAll();
        assertEquals(1, result.size());
        assertEquals("stick_bundle", result.get(0).id());
    }
}
