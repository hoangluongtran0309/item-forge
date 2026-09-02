package com.hoangluongtran0309.infrastructure.ai;

import java.util.Map;

import com.hoangluongtran0309.application.exception.AiItemGenerationException;
import com.hoangluongtran0309.application.exception.AiRequestException;
import com.hoangluongtran0309.application.port.AiItemGeneratorPort;
import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.infrastructure.config.ItemDefinitionMapper;

/**
 * Turns a natural-language description into an item draft. The prompt and the output schema
 * live here once, rather than once per provider, because every provider reaches them through
 * {@link StructuredAiClient}.
 */
public class AiItemGenerator implements AiItemGeneratorPort {

    private static final String SCHEMA_NAME = "submit_item_definition";
    private static final String SCHEMA_DESCRIPTION = "Submit the generated Minecraft item definition.";

    private static final String SYSTEM_PROMPT = "You are an expert Minecraft item designer for the ItemForge "
            + "plugin. Given a short description from a server admin, invent a fitting custom item. Keep lore to "
            + "1-3 short lines and include at most one ability unless the description clearly asks for more.";

    private static final String JSON_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "material": {
                  "type": "string",
                  "description": "A valid Bukkit/Minecraft Material enum name in UPPER_SNAKE_CASE, e.g. DIAMOND_SWORD, LEATHER_CHESTPLATE, FEATHER."
                },
                "display-name": {
                  "type": "string",
                  "description": "Item display name. May use & color codes, e.g. &cFire Sword."
                },
                "lore": {
                  "type": "array",
                  "items": { "type": "string" },
                  "description": "Lore lines shown under the item name. May use & color codes."
                },
                "abilities": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "type": { "type": "string", "enum": ["POTION_EFFECT", "DAMAGE_BONUS"] },
                      "trigger": { "type": "string", "enum": ["RIGHT_CLICK", "LEFT_CLICK", "ON_HIT", "ON_KILL", "ON_CONSUME"] },
                      "cooldown-seconds": { "type": "integer" },
                      "effect": {
                        "type": "string",
                        "enum": ["FIRE_RESISTANCE", "SLOWNESS", "SPEED", "NIGHT_VISION", "REGENERATION"],
                        "description": "Required only when type is POTION_EFFECT."
                      },
                      "duration-seconds": { "type": "integer", "description": "Required only when type is POTION_EFFECT." },
                      "bonus-percent": { "type": "number", "description": "Required only when type is DAMAGE_BONUS; must be greater than 0." }
                    },
                    "required": ["type", "trigger", "cooldown-seconds"]
                  }
                }
              },
              "required": ["material", "display-name", "lore", "abilities"]
            }
            """;

    private static final String SCHEMA_PROSE = "Respond with ONLY a single JSON object (no markdown fences, no "
            + "commentary) with exactly these keys: \"material\" (a valid Bukkit/Minecraft Material enum name in "
            + "UPPER_SNAKE_CASE, e.g. DIAMOND_SWORD, LEATHER_CHESTPLATE, FEATHER), \"display-name\" (string, may "
            + "use & color codes), \"lore\" (array of 1-3 short strings, may use & color codes), \"abilities\" "
            + "(array of objects, each with \"type\" one of POTION_EFFECT or DAMAGE_BONUS, \"trigger\" one of "
            + "RIGHT_CLICK, LEFT_CLICK, ON_HIT, ON_KILL, ON_CONSUME, \"cooldown-seconds\" (integer), and when type "
            + "is POTION_EFFECT also \"effect\" one of FIRE_RESISTANCE, SLOWNESS, SPEED, NIGHT_VISION, REGENERATION "
            + "plus \"duration-seconds\" (integer), or when type is DAMAGE_BONUS also \"bonus-percent\" (number "
            + "greater than 0)). Your entire reply must be valid JSON.";

    private final StructuredAiClient client;

    public AiItemGenerator(StructuredAiClient client) {
        this.client = client;
    }

    @Override
    public ItemDefinition generateDraft(String itemId, String description) {
        Map<String, Object> itemMap;
        try {
            itemMap = client.requestJsonObject(new StructuredAiRequest(SYSTEM_PROMPT, description, SCHEMA_NAME,
                    SCHEMA_DESCRIPTION, JSON_SCHEMA, SCHEMA_PROSE));
        } catch (AiRequestException e) {
            throw new AiItemGenerationException(e.getMessage());
        }

        try {
            return ItemDefinitionMapper.fromMap(itemId, itemMap);
        } catch (RuntimeException e) {
            throw new AiItemGenerationException("The generated item was not usable: " + e.getMessage());
        }
    }
}
