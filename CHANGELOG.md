# Changelog

All notable changes to ItemForge are documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Each released
section below is published verbatim as the notes of the matching GitHub Release.

## [Unreleased]

### Fixed

- `/itemforge reload` now re-reads the `ai` section of `config.yml`. Changing the provider, key or
  model, or turning AI on or off, used to be silently ignored until the server was restarted, even
  though the README said `reload` covered `config.yml`. The `resource-pack`, `hud` and
  `dashboard-api` settings still need a restart, and the README now says so.
- Narrowing a balance report to one id (`/itemforge analyze <id>`, or the box on the dashboard's
  Balance page) now narrows the AI findings too. They used to come back about every item in the
  config while the rule findings were correctly limited to the one asked about.

### Documentation

- Added [`docs/showcase/`](docs/showcase/README.md), a walkthrough of the plugin in game and of
  the dashboard, Texture Studio, balance analysis, and AI generation, in screenshots and GIFs
  captured from a real run.

## [1.0.0] - 2026-09-02

First public release.

ItemForge lets a server admin define custom items, armor, blocks, and crafting recipes in YAML,
and generates and serves the matching resource pack automatically. It is a free, open-source
alternative to ItemsAdder and Oraxen, built on a hexagonal architecture with first-class support
for both the pre-1.21.4 and post-1.21.4 model systems.

**Requirements:** Paper 1.20.5 or newer, Java 21. Tested from Paper 1.20.6 through 1.21.11.

### Custom items

- Items are defined in `items.yml`: base material, `custom-model-data`, display name, and lore,
  with `&`-colour codes supported throughout.
- Right-click and left-click abilities, currently `POTION_EFFECT`, with per-player cooldowns.
- `/itemforge give`, `reload`, and `list` for handing out items, applying config changes without a
  restart, and auditing everything currently registered.

### Resource pack automation

- Model JSON and texture packaging are generated from your config — no hand-written pack files.
- The pack is zipped, SHA-1 hashed, and served by a built-in HTTP server, so no external file host
  is required. Players receive it on join, and `/itemforge reload` rebuilds and re-sends it.
- A missing texture never fails the build: the entry falls back to a placeholder and the reason is
  logged, so one bad file cannot take down the whole pack.

### Multi-version compatibility

- The server version is detected at startup and the rendering strategy is chosen for you.
- **Legacy** (`CustomModelData` plus model `overrides`) on Paper below 1.21.4.
- **Modern** (the `item_model` component plus standalone item model files) on Paper 1.21.4 and
  above.
- The same YAML works unchanged on both. There is no upper version ceiling — newer Paper releases
  keep using the Modern strategy automatically.

### Custom armor

- Armor is defined in `armor.yml` with its slot and an optional `armor-asset-id` that groups a
  multi-piece set behind one worn texture.
- Each piece carries two distinct textures: a flat inventory icon (`textures/armor/<id>.png`) and
  the 64x32 layers drawn on the player when worn
  (`textures/armor/<armor-asset-id>_layer_{1,2}.png`).
- On 1.21.4+ this uses the modern equippable component and generated equipment assets; on older
  servers it falls back to overriding the vanilla armor layer paths.
- Ability cooldowns are surfaced through a BossBar, and ability triggers through the ActionBar.

### Crafting recipes

- Shaped and shapeless recipes in `recipes.yml`, producing either a custom item or a vanilla one.
- Ingredients and results resolve as a custom item id first, then a custom armor id, then a vanilla
  material — so a custom ingredient matches that exact item and nothing else sharing its material.

### Custom blocks

- Blocks are defined in `blocks.yml` as reskinned note blocks, keyed by a unique instrument and
  note pair, each with its own texture, display name, and drop item.
- Listeners cover placement, breaking, piston movement, explosions, and vanilla's automatic
  instrument recalculation, with placed blocks tracked by world coordinates so they survive
  restarts.

### Balance analysis

- `/itemforge analyze [<id>]` reviews your items, armor, and recipes and reports what is likely to
  distort gameplay: a potion effect that lasts as long as its own cooldown, an ability with no
  cooldown at all, a damage bonus worth more than a material tier, an item far stronger than its
  peers, one that costs almost nothing to craft, and armor sets built from mixed material tiers. It
  also flags abilities that load without error but never fire — the `ON_HIT`/`ON_KILL`/`ON_CONSUME`
  triggers and `DAMAGE_BONUS` listed under Known gaps below.
- The findings are produced by rules, not by a model: they cost nothing, need no API key, and are
  the same every run. Narrowing the report to a single id does not change what counts as an outlier,
  because every item is still scored — only the reporting is narrowed.
- With `ai.enabled: true` the configured provider reviews the config as well and adds the judgement
  calls rules cannot make: progression gaps, items that make others pointless, themes that do not
  match their power. If that call fails, the rule findings are still reported, with a line
  explaining what went wrong.
- The report is also available at `GET /api/balance` (and `/api/balance/<id>`) and on the **Balance**
  page of the web dashboard.

### Optional: web dashboard

Disabled by default; see [`dashboard/README.md`](dashboard/README.md).

- A separate Spring Boot application for managing items, armor, blocks, and recipes in a browser
  instead of hand-editing YAML, talking to the plugin over a token-authenticated REST API.
- **Texture Studio** — an in-browser pixel editor for every texture type, with live preview (flat
  icon, 3D cube for blocks, humanoid model rendering both armor layers together), UV overlay,
  drawing tools, keyboard shortcuts, and local drafts that survive a reload. Available as a modal
  or a deep-linkable full-page workspace at `/studio`.
- **Reference library** — browse and search resource packs you import yourself at `/reference` for
  use as drawing references. ItemForge bundles and downloads no Minecraft assets; imported packs
  stay on your own machine. Extraction is capped by total size, per-entry size, and entry count to
  guard against zip bombs.
- **Balance** — the balance report grouped by severity, with a re-run button and a box to narrow it
  to a single id.
- Visual 3x3 grid editor for shaped recipes, per-field validation on every form, and a sidebar
  layout that collapses to a slide-over on mobile.

### Optional: AI-assisted item generation

- `/itemforge generate <id> <description>` drafts a new item from a natural-language description.
- Four providers are supported — Claude, ChatGPT, DeepSeek, and Gemini — selected via
  `ai.provider`.
- All four run through one shared structured-output client, so a new provider is one adapter class
  rather than another copy of the prompt and HTTP handling, and the balance analyzer reuses the same
  seam.
- `ai.<provider>.timeout-seconds` bounds both the connect and the read, so a provider that accepts
  the connection and then stops responding cannot hang the request.
- Disabled by default (`ai.enabled: false`) and requires your own billed API key. This flag gates
  only the AI half of `/itemforge analyze`; the rule findings are produced either way.

### Optional: Docker deployment

The plain Java install remains the primary, supported path; Docker is an alternative.

- `docker-compose.yml` plus `./install.sh` bring up a Paper server and the dashboard together,
  wired up automatically.
- The installer checks every prerequisite up front, generates a secure API token and a BCrypt admin
  password hash, and is safe to re-run without losing your configuration.
- The Minecraft version you type is checked against Paper's published builds before anything is
  started, so a version with no such build is caught at the prompt instead of leaving the server
  container to crash-loop on it. The check falls back to trusting your input when the Paper API
  cannot be reached, so the installer still works offline.
- Readiness is taken from Paper's own `Done (...)` line, not from an open TCP port. Docker publishes
  the port as soon as the container is created, so a port probe reports success while Paper is still
  downloading — and even while the container is crash-looping. When the container cannot start, the
  installer fails with the actual cause.
- No extra system package is needed for password hashing: the installer tries `htpasswd`, then a
  throwaway `httpd:2.4-alpine` container, then `python3` with the `bcrypt` module, probing each
  before committing to it. `$2a$`, `$2b$`, and `$2y$` hashes are all accepted.
- Bind mounts carry the `:z` relabel flag so the stack starts on SELinux-enforcing hosts (Fedora,
  RHEL, CentOS) rather than crash-looping on `Permission denied`.
- Persistent state lives in `./server-data` and `./dashboard-data` next to `docker-compose.yml`;
  both are worth including in your backups.
- **On Windows**, run `install.sh` from Git Bash or WSL — it is a bash script and will not run
  under `cmd.exe` or PowerShell.

### Example content

- The shipped `items.yml`, `armor.yml`, `blocks.yml`, and `recipes.yml` define a complete "Void
  Netherite" set — sword, pickaxe, a four-piece armor set, and a block — that exercises every
  supported config feature.
- The PNGs backing it are in [`examples/textures/`](examples/textures/), laid out exactly as the
  plugin's texture folder, so the whole directory can be copied into a server as-is. Every shipped
  sample has a real texture; nothing references a file that does not exist.

### Security

- Every security-sensitive default ships as an explicit `CHANGE_ME` placeholder rather than a
  working value: `resource-pack.host`, all AI provider API keys, `dashboard-api.api-key`, and the
  dashboard admin password hash.
- Three of those are enforced rather than merely warned about, while the placeholder is still in
  place: the plugin refuses to start the resource-pack server, it refuses to start the dashboard API
  server (which would otherwise turn `CHANGE_ME` into a valid bearer token for an API that can
  create, edit, and delete real items), and the dashboard refuses all logins.
- The dashboard API and the AI integration are both off by default.
- `.gitattributes` pins shell scripts to LF endings, so a clone on Windows (where Git defaults to
  `core.autocrlf=true`) does not produce an `install.sh` that bash refuses to run.

### Known gaps

These are documented rather than hidden, and are planned for future releases. The first two are also
reported by `/itemforge analyze` against your own config, so an ability that will never fire is
surfaced at runtime rather than failing silently:

- `ON_HIT`, `ON_KILL`, and `ON_CONSUME` exist in the ability data model but are not dispatched at
  runtime. Configuring them loads without error but never fires.
- The `DAMAGE_BONUS` ability type parses and validates, but applying it is not implemented yet — it
  logs a warning instead.
- Custom blocks cannot be used as a crafting ingredient or result, and cannot drop themselves;
  `drop-item-id` must be a custom item id or a vanilla material.
- On Legacy-strategy servers only one custom armor set per vanilla material family can be rendered,
  because the worn texture overwrites a vanilla global path.
- Custom-block world-state edge cases (piston pushes, explosions, chunk reloads) are handled in the
  implementation but not yet covered by an automated regression suite. Back up your world before
  heavy use and report anything odd via
  [GitHub Issues](https://github.com/hoangluongtran0309/item-forge/issues).

[Unreleased]: https://github.com/hoangluongtran0309/item-forge/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/hoangluongtran0309/item-forge/releases/tag/v1.0.0
