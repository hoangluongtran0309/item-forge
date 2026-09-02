# Example textures — the "Void Netherite" set

The PNGs here back the sample definitions shipped in `src/main/resources/items.yml`,
`armor.yml` and `blocks.yml`. They are reference assets: the plugin does **not** bundle them
into the JAR, so a fresh install has the sample configs but no textures until these files are
copied into the server.

## Install

This folder mirrors the runtime layout exactly, so it can be copied over as-is:

```bash
cp -r examples/textures/. /path/to/server/plugins/ItemForge/textures/
```

Then run `/itemforge reload` (or restart the server) to rebuild the resource pack.

## What each file is for

ItemForge locates textures by **id**, not by a path written in the config — renaming a file
means renaming the matching id.

| File | Referenced by | Config field |
| --- | --- | --- |
| `void_sword.png` | `items.yml` → `void_sword` | the item id |
| `void_pickaxe.png` | `items.yml` → `void_pickaxe` | the item id |
| `armor/void_helmet.png` | `armor.yml` → `void_helmet` | the armor id |
| `armor/void_chestplate.png` | `armor.yml` → `void_chestplate` | the armor id |
| `armor/void_leggings.png` | `armor.yml` → `void_leggings` | the armor id |
| `armor/void_boots.png` | `armor.yml` → `void_boots` | the armor id |
| `armor/void_armor_layer_1.png` | all four armor pieces | `armor-asset-id: void_armor` |
| `armor/void_armor_layer_2.png` | the leggings overlay | `armor-asset-id: void_armor` |
| `blocks/void_netherite_block.png` | `blocks.yml` → `void_netherite_block` | `texture-id` |

Note that item icons live flat in `textures/`, not in a `textures/items/` subfolder, and that
an armor piece needs two separate textures: `armor/<id>.png` is the flat inventory icon, while
`armor/<armor-asset-id>_layer_{1,2}.png` are the textures mapped onto the player model when the
piece is worn.

## Size requirements

| Kind | Requirement |
| --- | --- |
| Item icons, armor icons, block textures | Square, with sides a multiple of 16 (these are 16×16) |
| Armor layers (`_layer_1`, `_layer_2`) | Exactly 64×32 |

A missing texture is not fatal: the pack is still generated, the affected entry just falls back
to a placeholder and the server logs a warning.
