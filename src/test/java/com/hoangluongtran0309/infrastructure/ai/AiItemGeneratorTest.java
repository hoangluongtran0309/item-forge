package com.hoangluongtran0309.infrastructure.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import com.hoangluongtran0309.application.exception.AiItemGenerationException;
import com.hoangluongtran0309.application.exception.AiRequestException;
import com.hoangluongtran0309.domain.model.DamageBonusAbilityDefinition;
import com.hoangluongtran0309.domain.model.ItemDefinition;

class AiItemGeneratorTest {

    @Test
    void generateDraftMapsTheProviderResponseIntoAnItemDefinition() {
        RecordingAiClient client = new RecordingAiClient(Map.of(
                "material", "NETHERITE_PICKAXE",
                "display-name", "Power Axe",
                "lore", List.of("A heavy axe."),
                "abilities", List.of(Map.of(
                        "type", "DAMAGE_BONUS",
                        "trigger", "ON_HIT",
                        "cooldown-seconds", 10,
                        "bonus-percent", 15.5))));

        ItemDefinition result = new AiItemGenerator(client).generateDraft("void_pickaxe", "A heavy axe");

        assertEquals("void_pickaxe", result.id());
        assertEquals("NETHERITE_PICKAXE", result.material());
        assertEquals("Power Axe", result.displayName());
        assertEquals(List.of("A heavy axe."), result.lore());
        DamageBonusAbilityDefinition ability = assertInstanceOf(DamageBonusAbilityDefinition.class,
                result.abilities().get(0));
        assertEquals(15.5, ability.bonusPercent());
    }

    @Test
    void generateDraftSendsTheDescriptionAsTheUserMessageAndAValidSchema() {
        RecordingAiClient client = new RecordingAiClient(Map.of("material", "STICK"));

        new AiItemGenerator(client).generateDraft("twig", "A plain twig");

        assertEquals("A plain twig", client.lastRequest.userMessage());
        assertEquals("submit_item_definition", client.lastRequest.schemaName());

        Map<String, Object> schema = new Yaml().load(client.lastRequest.jsonSchema());
        assertEquals("object", schema.get("type"));
        assertTrue(client.lastRequest.schemaProse().contains("JSON"));
    }

    @Test
    void generateDraftReportsAProviderFailureAsAGenerationFailure() {
        StructuredAiClient failing = request -> {
            throw new AiRequestException("Claude API returned HTTP 401");
        };

        AiItemGenerationException exception = assertThrows(AiItemGenerationException.class,
                () -> new AiItemGenerator(failing).generateDraft("void_sword", "A sword"));
        assertTrue(exception.getMessage().contains("HTTP 401"));
    }

    @Test
    void generateDraftReportsAnUnusableResponseInsteadOfLeakingTheMapperError() {
        RecordingAiClient client = new RecordingAiClient(Map.of("display-name", "No material here"));

        assertThrows(AiItemGenerationException.class,
                () -> new AiItemGenerator(client).generateDraft("void_sword", "A sword"));
    }

    private static final class RecordingAiClient implements StructuredAiClient {

        private final Map<String, Object> response;
        private StructuredAiRequest lastRequest;

        private RecordingAiClient(Map<String, Object> response) {
            this.response = response;
        }

        @Override
        public Map<String, Object> requestJsonObject(StructuredAiRequest request) {
            this.lastRequest = request;
            return response;
        }
    }
}
