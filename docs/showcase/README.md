# ItemForge in action

Everything on this page was captured from a real run: Paper 1.21.11 started through the
[Quick Docker Setup](../../README.md#quick-docker-setup-optional), the sample "Void Netherite"
content that ships with the plugin, and the textures from
[`examples/textures/`](../../examples/textures/). A few extra items (`storm_blade`, `ember_staff`,
`frost_wand`) were created on camera to show the dashboard and the AI features. Nothing is mocked
up.

For how to install and configure any of it, see the [main README](../../README.md).

- [In game](#in-game)
- [Web dashboard](#web-dashboard)
- [Texture Studio](#texture-studio)
- [Balance analysis](#balance-analysis)
- [AI-assisted item generation](#ai-assisted-item-generation)
- [Console output](#console-output)
- [One config, two rendering strategies](#one-config-two-rendering-strategies)

## In game

Joining the server offers the generated resource pack, served by the plugin's own HTTP server —
no external file host involved.

![Resource pack prompt on join](ingame-pack-prompt.jpg)

### Items and abilities

`void_sword` from `items.yml`: a custom texture, a coloured name, lore lines, and a right-click
ability.

![Void Netherite Sword tooltip in the inventory](ingame-void-sword.jpg)

Right-clicking applies the potion effect and starts the per-player cooldown, shown as a BossBar
that drains until the ability is ready again.

![Right-clicking the sword grants Speed and starts the cooldown bar](ingame-ability.gif)

### Armor

The four `void_*` pieces share one `armor-asset-id`, so they render as a single set when worn.

![Full Void Netherite armor set in third person](ingame-void-armor.jpg)

### Recipes

The `void_sword` recipe from `recipes.yml` in a vanilla crafting table.

![Crafting the Void Netherite Sword](ingame-crafting.jpg)

### Custom blocks

`void_netherite_block` is a reskinned note block. Placing it shows the custom texture; breaking
it drops whatever `drop-item-id` names — a vanilla netherite block here.

![Placing and breaking the custom block](ingame-block.gif)

### From the browser to the game

This sword was created, drawn, and given a recipe entirely in the [dashboard](#web-dashboard) —
the GIFs further down show each step.

![The Storm Blade held in game](ingame-storm-blade.jpg)

### Commands

`/itemforge list` shows everything registered (the end of the list is visible here).

![Output of /itemforge list](ingame-list.jpg)

`/itemforge analyze` prints the balance report in chat, most severe first.

![Output of /itemforge analyze](ingame-analyze.jpg)

`/itemforge generate` creates an item from a description (requires `ai.enabled: true`). It
fills in the definition only — the new item has no texture until you draw or upload one.

![Output of /itemforge generate](ingame-generate.jpg)

## Web dashboard

The dashboard is optional — every screen below edits the same YAML files you could edit by hand.
See [dashboard/README.md](../../dashboard/README.md) for setup.

![Overview page with item, armor, block and recipe counts](dashboard-overview.jpg)

### Items

Items are listed with their texture, base material, and ability types.

![Item list](dashboard-items.jpg)

Creating one is a single form: id, material, display name, lore, and any number of ability rows.

![Creating an item with a right-click potion effect](dashboard-item-create.gif)

| Identity and texture panel | Details and abilities |
| --- | --- |
| ![Item form, top](dashboard-item-form.jpg) | ![Item form, abilities](dashboard-item-abilities.jpg) |

### Armor

An armor piece has two kinds of texture: its own inventory icon, and the body and leggings
layers it shares with every other piece using the same `armor-asset-id`.

![Armor list](dashboard-armor-list.jpg)

![Armor form with icon, body and leggings texture panels](dashboard-armor.jpg)

### Blocks

Custom blocks are reskinned note blocks, so each one claims an instrument and note pair.

![Custom block form](dashboard-blocks.jpg)

### Recipes

Shaped recipes are laid out on a 3×3 grid; each letter maps to an ingredient below it.
Ingredients and results can be vanilla materials or your own item and armor ids.

![Creating a shaped recipe on the 3×3 grid](dashboard-recipe-grid.gif)

![Recipe list](dashboard-recipes.jpg)

### Reference library

Import a resource pack you own and browse its textures while you draw. The pack shown here is
the one ItemForge generated itself — the library ships with no Minecraft assets and downloads
none.

![Reference library after importing a pack](reference-library.jpg)

### Light theme and login

| Light theme | Login |
| --- | --- |
| ![Overview in the light theme](dashboard-light.jpg) | ![Login page](dashboard-login.jpg) |

## Texture Studio

A pixel editor in the browser. Saving uploads the texture to the plugin and rebuilds the
resource pack in one step.

![Drawing a sword texture and saving it](studio-item.gif)

The preview follows what you are editing:

| Item — flat icon | Block — rotating cube | Armor — worn on a humanoid model |
| --- | --- | --- |
| ![Item texture in the Studio](studio-item.jpg) | ![Block texture with cube preview](studio-block.jpg) | ![Armor body layer with humanoid preview](studio-armor.jpg) |

## Balance analysis

The same report as `/itemforge analyze`. Findings tagged `Rule` come from deterministic rules and
need no API key. Findings tagged `AI` appear only with `ai.enabled: true` and are a model's
opinion — weigh them accordingly.

![Balance report grouped by severity](balance-report.jpg)

Type an id and re-run to narrow the rule findings to a single item. Only the results are swapped
in; the page does not reload.

![Narrowing the report to one id and re-running](balance-rerun.gif)

If the AI call fails, the rule findings still arrive, with one extra line saying what went wrong.
This is the report with a placeholder API key:

![Balance report when the AI provider rejects the key](balance-ai-failsoft.jpg)

See [Balance analysis](../../README.md#balance-analysis) for what each rule checks.

## AI-assisted item generation

Off by default, and it makes billed API calls when on. Describe the item, and the configured
provider fills in the material, name, lore, and abilities for you to review before saving.

![Generating an item from a text description](dashboard-ai-generate.gif)

The generated item is ordinary config, so it goes through balance analysis like anything else.
The staff above came back with a 60-second effect on a 30-second cooldown, which the
`PERMANENT_EFFECT` rule reports.

## Console output

`/itemforge list`, run from the server console against the shipped sample content:

```
- void_pickaxe
    - RIGHT_CLICK: PotionEffectAbilityDefinition (cooldown 45s)
- void_sword
    - RIGHT_CLICK: PotionEffectAbilityDefinition (cooldown 30s)
- [armor] void_boots
- [armor] void_chestplate
- [armor] void_leggings
- [armor] void_helmet
- [recipe] void_pickaxe
- [recipe] void_sword
- [recipe] void_helmet
- [recipe] void_sword_salvage
- [block] void_netherite_block
```

## One config, two rendering strategies

Minecraft 1.21.4 replaced `CustomModelData` overrides with item model definitions. ItemForge
picks the matching strategy from the server version at startup, so the same YAML works on both
sides. These are the startup lines from two servers running the same jar and the same sample
config:

```
# Paper 1.21.1
[ItemForge] Detected server version 1.21.1 -> using LegacyModelStrategy / LegacyArmorModelStrategy / LegacyBlockModelStrategy

# Paper 1.21.11
[ItemForge] Detected server version 1.21.11 -> using ModernModelStrategy / ModernArmorModelStrategy / ModernBlockModelStrategy
```

What differs is the resource pack each one generates. For `void_sword`
(`material: NETHERITE_SWORD`, `custom-model-data: 1001`):

**Legacy, below 1.21.4** — an override is added to the vanilla item's own model, keyed on the
`custom-model-data` number:

```jsonc
// assets/minecraft/models/item/netherite_sword.json
{
  "parent": "item/handheld",
  "textures": {
    "layer0": "minecraft:item/netherite_sword"
  },
  "overrides": [
    { "predicate": { "custom_model_data": 1001 }, "model": "itemforge:custom/void_sword" }
  ]
}
```

**Modern, 1.21.4 and above** — the item gets a standalone definition in ItemForge's own
namespace, and no vanilla item model is touched:

```jsonc
// assets/itemforge/items/void_sword.json
{
  "model": {
    "type": "minecraft:model",
    "model": "itemforge:custom/void_sword"
  }
}
```

Both point at the same model and texture (`itemforge:custom/void_sword`).

Armor follows the same split. Legacy writes the worn texture over the vanilla path
(`assets/minecraft/textures/models/armor/netherite_layer_1.png`), which is why only one custom
set per vanilla material family can be rendered there. Modern writes an equipment asset of its
own (`assets/itemforge/equipment/void_armor.json`) and has no such limit.

The in-game screenshots on this page were all taken on the Modern side (Paper 1.21.11). The
Legacy side is shown here through its log line and generated files only.
