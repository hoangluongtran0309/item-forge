## What this changes

<!-- What the change does, and why. Link the issue if there is one: Closes #123 -->

## Type of change

- [ ] `feat` — new functionality
- [ ] `fix` — bug fix
- [ ] `docs` — documentation only
- [ ] `test` — tests only
- [ ] `chore` / `refactor` — no behaviour change

## Checklist

- [ ] `mvn verify` passes for every project I touched (root plugin and/or `dashboard/`)
- [ ] No class under `domain/` imports `org.bukkit.*` — that layer stays pure Java
- [ ] `application/` talks to the outside world through ports, never the Bukkit API directly
- [ ] Anything affecting item appearance goes through `ItemModelStrategy`, with no version branch
      hardcoded into business logic
- [ ] New items, armor, blocks, recipes or abilities are defined in YAML, not hardcoded in Java

## Manual server testing

Does this touch `infrastructure/bukkit/**`?

- [ ] No — unit tests cover it
- [ ] Yes — and I tested it manually on a real Paper server

If yes, state the Paper version tested and what you verified in game:

<!-- e.g. "Paper 1.21.11 and 1.20.6: /itemforge reload rebuilt the pack, the sword rendered
     correctly on both, right-click ability fired." -->
