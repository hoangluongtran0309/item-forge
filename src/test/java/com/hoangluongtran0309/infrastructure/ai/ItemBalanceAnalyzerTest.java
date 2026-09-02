package com.hoangluongtran0309.infrastructure.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import com.hoangluongtran0309.application.exception.AiBalanceAnalysisException;
import com.hoangluongtran0309.application.exception.AiRequestException;
import com.hoangluongtran0309.domain.balance.BalanceAnalysisRequest;
import com.hoangluongtran0309.domain.balance.BalanceFinding;
import com.hoangluongtran0309.domain.balance.BalanceReport;
import com.hoangluongtran0309.domain.balance.BalanceSeverity;
import com.hoangluongtran0309.domain.balance.FindingSource;
import com.hoangluongtran0309.domain.model.ItemDefinition;

class ItemBalanceAnalyzerTest {

    private static final Logger LOGGER = Logger.getAnonymousLogger();

    @Test
    void theResponseBecomesAReportWhoseFindingsAreMarkedAsComingFromTheAi() {
        RecordingAiClient client = new RecordingAiClient(Map.of(
                "summary", "The sword outclasses everything else.",
                "findings", List.of(Map.of(
                        "target-id", "void_sword",
                        "severity", "WARNING",
                        "issue", "It makes the axe pointless.",
                        "suggestion", "Give the axe a niche."))));

        BalanceReport report = analyzer(client).analyze(request(sword()), List.of());

        assertEquals("The sword outclasses everything else.", report.summary());
        BalanceFinding finding = report.findings().get(0);
        assertEquals("void_sword", finding.targetId());
        assertEquals(BalanceSeverity.WARNING, finding.severity());
        assertEquals(FindingSource.AI, finding.source());
    }

    @Test
    void theItemsAreSentInTheShapeTheAdminWritesThemIn() {
        RecordingAiClient client = new RecordingAiClient(emptyReport());

        analyzer(client).analyze(request(sword()), List.of());

        Map<String, Object> payload = new Yaml().load(client.lastRequest.userMessage());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
        assertEquals("void_sword", items.get(0).get("id"));
        assertEquals("NETHERITE_SWORD", items.get(0).get("material"));
        assertFalse(items.get(0).containsKey("custom-model-data"));
    }

    @Test
    void theRuleFindingsAreSentAsContextSoTheModelDoesNotRepeatThem() {
        RecordingAiClient client = new RecordingAiClient(emptyReport());
        BalanceFinding ruleFinding = BalanceFinding.rule("void_sword", BalanceSeverity.CRITICAL,
                "PERMANENT_EFFECT", "SPEED never expires.", "Raise the cooldown.");

        analyzer(client).analyze(request(sword()), List.of(ruleFinding));

        Map<String, Object> payload = new Yaml().load(client.lastRequest.userMessage());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sent = (List<Map<String, Object>>) payload.get("rule-findings");
        assertEquals("PERMANENT_EFFECT", sent.get(0).get("rule"));
        assertEquals("SPEED never expires.", sent.get(0).get("issue"));
    }

    @Test
    void narrowingToOneIdIsStatedInThePayload() {
        RecordingAiClient client = new RecordingAiClient(emptyReport());

        analyzer(client).analyze(request(sword()).scopedTo("void_sword"), List.of());

        Map<String, Object> payload = new Yaml().load(client.lastRequest.userMessage());
        assertEquals("void_sword", payload.get("report-only-on"));
    }

    @Test
    void anOversizedConfigIsSampledAndSaysSo() {
        RecordingAiClient client = new RecordingAiClient(emptyReport());
        List<ItemDefinition> manyItems = new ArrayList<>(IntStream
                .range(0, ItemBalanceAnalyzer.MAX_DEFINITIONS_PER_KIND + 25)
                .mapToObj(index -> new ItemDefinition("item_" + index, "IRON_SWORD", 1, "Item", List.of(), List.of()))
                .toList());
        BalanceAnalysisRequest oversized = BalanceAnalysisRequest.wholeConfig(manyItems, List.of(), List.of());

        ItemBalanceAnalyzer analyzer = analyzer(client);
        assertTrue(analyzer.isTruncated(oversized));
        analyzer.analyze(oversized, List.of());

        Map<String, Object> payload = new Yaml().load(client.lastRequest.userMessage());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
        assertEquals(ItemBalanceAnalyzer.MAX_DEFINITIONS_PER_KIND, items.size());
        assertTrue(payload.get("note").toString().contains("too large"));
    }

    @Test
    void aMalformedFindingIsSkippedRatherThanLosingTheWholeReport() {
        RecordingAiClient client = new RecordingAiClient(Map.of(
                "summary", "Mostly fine.",
                "findings", List.of(
                        Map.of("severity", "WARNING", "issue", "No target id on this one."),
                        Map.of("target-id", "void_sword", "severity", "WARNING", "issue", "This one is fine."))));

        BalanceReport report = analyzer(client).analyze(request(sword()), List.of());

        assertEquals(1, report.findings().size());
        assertEquals("This one is fine.", report.findings().get(0).issue());
    }

    @Test
    void aProviderFailureIsReportedAsABalanceAnalysisFailure() {
        StructuredAiClient failing = request -> {
            throw new AiRequestException("Gemini API returned HTTP 429");
        };

        AiBalanceAnalysisException exception = assertThrows(AiBalanceAnalysisException.class,
                () -> analyzer(failing).analyze(request(sword()), List.of()));
        assertTrue(exception.getMessage().contains("HTTP 429"));
    }

    @Test
    void theSchemaSentIsValidJsonAndTheProseMentionsJson() {
        RecordingAiClient client = new RecordingAiClient(emptyReport());

        analyzer(client).analyze(request(sword()), List.of());

        Map<String, Object> schema = new Yaml().load(client.lastRequest.jsonSchema());
        assertEquals("object", schema.get("type"));
        assertTrue(client.lastRequest.schemaProse().contains("JSON"));
    }

    private static ItemBalanceAnalyzer analyzer(StructuredAiClient client) {
        return new ItemBalanceAnalyzer(client, LOGGER);
    }

    private static Map<String, Object> emptyReport() {
        return Map.of("summary", "", "findings", List.of());
    }

    private static BalanceAnalysisRequest request(ItemDefinition... items) {
        return BalanceAnalysisRequest.wholeConfig(List.of(items), List.of(), List.of());
    }

    private static ItemDefinition sword() {
        return new ItemDefinition("void_sword", "NETHERITE_SWORD", 1001, "Void Sword", List.of("Dark."), List.of());
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
