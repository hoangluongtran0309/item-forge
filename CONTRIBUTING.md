# Contributing to ItemForge

Thanks for taking the time. This file covers everything you need to get a change merged: how the
repository is laid out, the rules the architecture depends on, and how a release is cut.

## What is in this repository

Two independent Maven projects:

| Path | What it is | Build |
| --- | --- | --- |
| `/` | The Paper plugin (`itemforge`), Java 21 | `mvn` (no wrapper, use your own Maven) |
| `/dashboard` | Optional Spring Boot web dashboard | `./mvnw` (wrapper included) |

They are not modules of a parent pom — build them separately.

## Getting set up

```bash
# Plugin
mvn clean verify                  # compile + run the unit tests
mvn clean package -DskipTests     # faster loop while developing
# -> target/itemforge-<version>.jar, copy into your server's plugins/ directory

# Dashboard
cd dashboard
./mvnw clean verify
./mvnw spring-boot:run            # http://localhost:8090
```

The dashboard build downloads its own Node and npm through `frontend-maven-plugin`; you do not
need Node installed.

Never commit a built jar.

## Branching model

| Branch | Purpose |
| --- | --- |
| `main` | Released code only. Every commit corresponds to a published version and carries a `vX.Y.Z` tag. |
| `develop` | Integration branch. This is what you branch from and target. |
| `feature/<short-name>` | New functionality |
| `fix/<short-name>` | Bug fixes |
| `release/<version>` | Created by the `prepare-release` workflow, never by hand |

```bash
git switch develop && git pull
git switch -c feature/on-consume-trigger
# ... work ...
git push -u origin feature/on-consume-trigger
# then open a PR into develop
```

Never open a PR straight into `main` — that path is reserved for releases.

## Commit messages

[Conventional Commits](https://www.conventionalcommits.org/), which also applies to PR titles:

```
feat: dispatch ON_CONSUME ability triggers
fix: fall back to the placeholder when an armor layer is missing
docs: document resource-pack.host in the README
test: cover shapeless recipes with a custom ingredient
chore: bump spring-boot to 4.1.1
```

## Architecture rules

ItemForge uses a hexagonal (ports and adapters) architecture. Three rules are non-negotiable, and a
PR that breaks one will not be merged:

1. **`domain/` must never import `org.bukkit.*`.** It is pure Java business logic and must be fully
   testable without a Minecraft server running.
2. **`application/` must never call the Bukkit API directly.** It coordinates use cases and reaches
   the outside world only through the interfaces in `application/port/`.
3. **`infrastructure/bukkit/` is the only place `org.bukkit.*` may be imported.** It adapts between
   domain models and `ItemStack`, `Player`, events, and so on.

If a change seems to require Bukkit inside the domain, the answer is a new port plus an adapter, not
an import.

Two more conventions worth stating:

- **Item appearance always goes through `ItemModelStrategy`.** Minecraft below 1.21.4 uses integer
  `CustomModelData` with model `overrides`; 1.21.4 and above use the `item_model` component with
  standalone model files. Business logic must never branch on the version itself — pick the strategy
  and let it decide.
- **Content lives in YAML, not in Java.** New items, armor, blocks, recipes and abilities are defined
  in `items.yml`, `armor.yml`, `blocks.yml` and `recipes.yml`. Hardcoding a definition in a Java class
  is a bug, not a shortcut.
- **Balance scoring is domain logic, not an AI feature.** The scorer and every rule live in
  `domain/balance/` with no Bukkit and no AI dependency, which is what makes them unit-testable and
  reproducible. `AiBalanceAnalyzerPort` is optional — it is null whenever `ai.enabled` is false — so
  a new rule goes in `BalanceRuleSet`, never in `infrastructure/ai/`, and the analysis must stay
  fully useful with no provider configured.

Also: English for all class, method and variable names, and for every comment. Comment only what the
code cannot say for itself. No Lombok — this plugin stays dependency-light.

## Testing

- **Domain layer** — plain JUnit 5, no server, no mocks of Bukkit types. Write these tests first.
- **Application layer** — JUnit 5 with fake implementations of the relevant ports.
- **Bukkit adapters** — MockBukkit where it fits, but MockBukkit is not a Paper server. If your
  change touches `infrastructure/bukkit/**`, test it by hand on a real Paper server and say so in
  the PR, including the Paper version you used.

Rendering changes deserve extra care: they behave differently on either side of the 1.21.4 boundary,
so verify on one server below it and one above.

## Opening a pull request

1. Target `develop`.
2. Fill in the PR template, including the manual-testing section.
3. CI must be green: the `plugin` and `dashboard` jobs both build and test on every PR.

## How a release is cut

Maintainers only, and no pom is ever edited by hand:

1. **Actions → Prepare release**, enter the version (e.g. `1.1.0`). The workflow bumps the version in
   `pom.xml`, `dashboard/pom.xml` and `dashboard/package.json`, opens a `## [1.1.0]` section in
   `CHANGELOG.md`, and opens a PR from `release/1.1.0` into `main`.
2. Fill in the changelog section on that branch, then merge the PR **with a merge commit** — not a
   squash, which would break every future back-merge.
3. Tag the merge commit:

   ```bash
   git switch main && git pull
   git tag -a v1.1.0 -m "ItemForge v1.1.0"
   git push origin v1.1.0
   ```

4. The `Release` workflow verifies that both poms match the tag and that the commit is on `main`,
   builds both jars, publishes them with `SHA256SUMS.txt` and the changelog section as release notes,
   and opens the back-merge PR from `main` into `develop`. Merge that last PR.

## License

By contributing you agree that your contributions are licensed under the [MIT License](LICENSE).
