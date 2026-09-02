package com.hoangluongtran0309.infrastructure.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.hoangluongtran0309.application.exception.AiBalanceAnalysisException;
import com.hoangluongtran0309.application.exception.AiRequestException;
import com.hoangluongtran0309.application.port.AiBalanceAnalyzerPort;
import com.hoangluongtran0309.domain.balance.BalanceAnalysisRequest;
import com.hoangluongtran0309.domain.balance.BalanceFinding;
import com.hoangluongtran0309.domain.balance.BalanceReport;
import com.hoangluongtran0309.domain.balance.FindingSource;
import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.domain.model.RecipeDefinition;
import com.hoangluongtran0309.infrastructure.config.ArmorDefinitionMapper;
import com.hoangluongtran0309.infrastructure.config.BalanceReportMapper;
import com.hoangluongtran0309.infrastructure.config.ItemDefinitionMapper;
import com.hoangluongtran0309.infrastructure.config.RecipeDefinitionMapper;
import com.hoangluongtran0309.infrastructure.json.JsonWriter;

/**
 * Asks an AI provider for the balance judgements the rules cannot make.
 *
 * <p>The config is sent in the same shape the admin writes it in, reusing the mappers behind
 * items.yml and armor.yml, so the model reads the fields the admin would edit rather than an
 * invented projection. What the rules already found is sent along too, and the prompt asks for
 * what those rules cannot see. That is a request, not a guarantee -- models restate rule findings
 * anyway -- so ItemBalanceAnalysisService discards the ones that come back about an already
 * covered id; the prompt says so, to keep the model from spending its output on them.
 */
public class ItemBalanceAnalyzer implements AiBalanceAnalyzerPort {

    private static final String SCHEMA_NAME = "submit_balance_report";
    private static final String SCHEMA_DESCRIPTION = "Submit the balance analysis of these custom items.";

    /**
     * How many definitions of each kind are sent. A config larger than this is analyzed from a
     * sample rather than refused, and the report says so.
     */
    static final int MAX_DEFINITIONS_PER_KIND = 60;

    private static final String SYSTEM_PROMPT = "You are balancing a Minecraft server's custom items for its "
            + "admin. You are given the item, armor and recipe definitions from an ItemForge config, plus the "
            + "findings a deterministic rule engine has already produced. Report only what those rules cannot "
            + "see: progression gaps, items that make others pointless, themes that do not match their power, "
            + "and recipes that are too cheap or too expensive for what they produce. Any finding you return "
            + "about an id that already appears in rule-findings is DISCARDED before the admin sees it, so "
            + "spend your findings on the other ids, or use the target-id \"*\" for something about the config "
            + "as a whole. Each finding must name a real id from the input and quote the actual numbers. Prefer "
            + "a handful of specific findings over an exhaustive list, and return no findings at all if the "
            + "config is well balanced.";

    private static final String JSON_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "summary": {
                  "type": "string",
                  "description": "One or two sentences on the overall state of the config."
                },
                "findings": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "target-id": {
                        "type": "string",
                        "description": "The id this finding is about, exactly as it appears in the input, or * for the config as a whole."
                      },
                      "severity": { "type": "string", "enum": ["INFO", "WARNING", "CRITICAL"] },
                      "issue": { "type": "string", "description": "What is wrong, quoting the actual numbers." },
                      "suggestion": { "type": "string", "description": "One concrete change the admin could make." }
                    },
                    "required": ["target-id", "severity", "issue", "suggestion"]
                  }
                }
              },
              "required": ["summary", "findings"]
            }
            """;

    private static final String SCHEMA_PROSE = "Respond with ONLY a single JSON object (no markdown fences, no "
            + "commentary) with exactly these keys: \"summary\" (one or two sentences on the overall state of the "
            + "config) and \"findings\" (an array of objects, each with \"target-id\" (an id from the input, or * "
            + "for the whole config), \"severity\" (one of INFO, WARNING, CRITICAL), \"issue\" (what is wrong, "
            + "quoting the actual numbers) and \"suggestion\" (one concrete change)). Your entire reply must be "
            + "valid JSON.";

    private final StructuredAiClient client;
    private final Logger logger;

    public ItemBalanceAnalyzer(StructuredAiClient client, Logger logger) {
        this.client = client;
        this.logger = logger;
    }

    @Override
    public BalanceReport analyze(BalanceAnalysisRequest request, List<BalanceFinding> ruleFindings) {
        Map<String, Object> response;
        try {
            response = client.requestJsonObject(new StructuredAiRequest(SYSTEM_PROMPT,
                    buildUserMessage(request, ruleFindings), SCHEMA_NAME, SCHEMA_DESCRIPTION, JSON_SCHEMA,
                    SCHEMA_PROSE));
        } catch (AiRequestException e) {
            throw new AiBalanceAnalysisException(e.getMessage());
        }

        return BalanceReportMapper.fromMap(response, FindingSource.AI, logger::warning);
    }

    String buildUserMessage(BalanceAnalysisRequest request, List<BalanceFinding> ruleFindings) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", itemsToMaps(request.items()));
        payload.put("armor", armorToMaps(request.armor()));
        payload.put("recipes", recipesToMaps(request.recipes()));
        payload.put("rule-findings", ruleFindingsToMaps(ruleFindings));

        if (request.isScoped()) {
            payload.put("report-only-on", request.targetId());
        }
        if (isTruncated(request)) {
            payload.put("note", "This config was too large to send in full; only the first "
                    + MAX_DEFINITIONS_PER_KIND + " definitions of each kind are included.");
        }

        return JsonWriter.write(payload);
    }

    /**
     * @return true when the payload had to be cut down, which the caller may want to disclose
     */
    boolean isTruncated(BalanceAnalysisRequest request) {
        return request.items().size() > MAX_DEFINITIONS_PER_KIND
                || request.armor().size() > MAX_DEFINITIONS_PER_KIND
                || request.recipes().size() > MAX_DEFINITIONS_PER_KIND;
    }

    private static List<Map<String, Object>> itemsToMaps(List<ItemDefinition> items) {
        List<Map<String, Object>> maps = new ArrayList<>();
        for (ItemDefinition item : capped(items)) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.id());
            map.putAll(ItemDefinitionMapper.toMap(item));
            // The model has no use for a texture id and it only spends tokens.
            map.remove("custom-model-data");
            maps.add(map);
        }
        return maps;
    }

    private static List<Map<String, Object>> armorToMaps(List<ArmorDefinition> armor) {
        List<Map<String, Object>> maps = new ArrayList<>();
        for (ArmorDefinition piece : capped(armor)) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", piece.id());
            map.putAll(ArmorDefinitionMapper.toMap(piece));
            map.remove("custom-model-data");
            maps.add(map);
        }
        return maps;
    }

    private static List<Map<String, Object>> recipesToMaps(List<RecipeDefinition> recipes) {
        List<Map<String, Object>> maps = new ArrayList<>();
        for (RecipeDefinition recipe : capped(recipes)) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", recipe.id());
            map.putAll(RecipeDefinitionMapper.toMap(recipe));
            maps.add(map);
        }
        return maps;
    }

    private static List<Map<String, Object>> ruleFindingsToMaps(List<BalanceFinding> findings) {
        List<Map<String, Object>> maps = new ArrayList<>();
        for (BalanceFinding finding : capped(findings)) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("target-id", finding.targetId());
            map.put("severity", finding.severity().name());
            map.put("rule", finding.rule());
            map.put("issue", finding.issue());
            maps.add(map);
        }
        return maps;
    }

    private static <T> List<T> capped(List<T> values) {
        return values.size() <= MAX_DEFINITIONS_PER_KIND
                ? values
                : values.subList(0, MAX_DEFINITIONS_PER_KIND);
    }
}
