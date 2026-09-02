package com.hoangluongtran0309.infrastructure.resourcepack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class EquipmentAssetGenerator {

    // WARNING: this is our current best understanding of Mojang's equipment asset JSON
    // format (1.21.2+) and has NOT been verified against a real client. If the format
    // turns out to be wrong, only this class needs fixing -- nothing else depends on it.
    //
    // Takes an armorAssetId (rather than an individual ArmorDefinition) because several
    // pieces -- a full set of four, say -- can share a single JSON file.
    public void writeEquipmentAsset(String armorAssetId, String namespace, Path outputDir,
            boolean includeLeggingsLayer) throws IOException {
        Path target = outputDir.resolve("assets/" + namespace + "/equipment/" + armorAssetId + ".json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, buildJson(armorAssetId, namespace, includeLeggingsLayer));
    }

    private String buildJson(String armorAssetId, String namespace, boolean includeLeggingsLayer) {
        String textureRef = namespace + ":" + armorAssetId;
        String leggingsLayer = includeLeggingsLayer
                ? """
                        ,
                          "humanoid_leggings": [ { "texture": "%s" } ]"""
                        .formatted(textureRef)
                : "";

        return """
                {
                  "layers": {
                    "humanoid": [ { "texture": "%s" } ]%s
                  }
                }
                """.formatted(textureRef, leggingsLayer);
    }
}
