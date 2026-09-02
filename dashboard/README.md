# ItemForge Dashboard

A web-based admin UI for [ItemForge](../README.md) — create, edit, and delete custom items,
armor, recipes, and blocks through a browser instead of hand-editing YAML, draw their textures
in a built-in pixel editor, and read the balance report for everything you have defined. It's a
separate Spring Boot application (Thymeleaf + HTMX server-rendered UI) that talks to the
plugin's `dashboard-api` over HTTP.

**This is entirely optional.** ItemForge runs fully standalone from in-game commands and YAML
files without it — only set this up if you specifically want a web UI for content management.

## ⚠️ Security warning

Read this before starting the dashboard, not after:

- **Do not expose the dashboard directly to the internet.** It has no built-in HTTPS, rate
  limiting, or brute-force protection. Put it behind a reverse proxy (nginx, Caddy, Traefik)
  that terminates HTTPS, or keep it reachable only over VPN/LAN.
- **Never use the default/placeholder values** for `ITEMFORGE_API_TOKEN` or
  `DASHBOARD_ADMIN_PASSWORD_HASH` in anything but local testing. The plugin-side
  `dashboard-api.api-key` and this dashboard's `ITEMFORGE_API_TOKEN` must be the same real
  secret — if you leave either at its placeholder, login/API auth is either broken or (in the
  worst case) trivially guessable.
- The dashboard, once authenticated, can create/edit/delete real items and upload textures on
  your live server — treat its admin login with the same care as server console/RCON access.

## Prerequisites

- The ItemForge plugin already running on your Paper server, with `dashboard-api.enabled: true`
  and a real (non-`CHANGE_ME`) `dashboard-api.api-key` set in `config.yml`.
- Java 21+ (for the Java route) or Docker (for the Docker route).

## Option A — Run with Java (recommended default)

1. Build the jar:
   ```bash
   cd dashboard
   mvn clean package -Prelease -DskipTests
   ```
   (The `release` profile builds minified frontend assets. Omitting `-DskipTests` also runs
   the dashboard's own test suite.) The jar appears as `target/dashboard-*.jar`.
2. Generate a BCrypt hash for your admin password. Use whichever of these you have — all three
   produce a hash Spring Security accepts, and reading the password from stdin keeps it out of
   your shell history and the process list:
   ```bash
   # a) htpasswd (apache2-utils on Debian/Ubuntu, httpd-tools on Fedora/RHEL)
   htpasswd -inBC 10 "" <<< 'your-real-password' | tr -d ':\n'

   # b) no htpasswd installed? hash it in a throwaway container instead
   docker run --rm -i httpd:2.4-alpine htpasswd -inBC 10 "" <<< 'your-real-password' | tr -d ':\n'

   # c) or with python3 and the bcrypt module
   python3 -c 'import bcrypt;print(bcrypt.hashpw(b"your-real-password",bcrypt.gensalt(10)).decode())'
   ```
   Copy the output — it's your `DASHBOARD_ADMIN_PASSWORD_HASH` value. Never store the plaintext
   password anywhere. (`install.sh` does this for you automatically, picking the first of the
   three that works.)
3. Generate a shared API token and set it on **both** sides:
   ```bash
   openssl rand -hex 32
   ```
   Set this value as `dashboard-api.api-key` in the plugin's `config.yml`, and as
   `ITEMFORGE_API_TOKEN` below.
4. Run the jar with the required environment variables set (see [table](#environment-variables)
   below):
   ```bash
   ITEMFORGE_API_BASE_URL=http://localhost:8081 \
   ITEMFORGE_API_TOKEN=<the token from step 3> \
   DASHBOARD_ADMIN_USERNAME=admin \
   DASHBOARD_ADMIN_PASSWORD_HASH='<the hash from step 2>' \
   java -jar target/dashboard-*.jar
   ```
5. Visit `http://localhost:8090` and log in.

## Option B — Run with Docker

If you're already using ItemForge's [Quick Docker Setup](../README.md#quick-docker-setup-optional)
(`./install.sh` at the repo root), the dashboard container is built and wired up for you
automatically — `ITEMFORGE_API_TOKEN` and `DASHBOARD_ADMIN_PASSWORD_HASH` are generated and
injected via `.env`, and `ITEMFORGE_API_BASE_URL` is set to reach the plugin container over the
internal Docker network. No manual steps needed beyond running `./install.sh` from the repo
root.

To build/run the dashboard container standalone instead:

```bash
docker build -t itemforge-dashboard ./dashboard
docker run -p 8090:8090 \
  -e ITEMFORGE_API_BASE_URL=http://host.docker.internal:8081 \
  -e ITEMFORGE_API_TOKEN=<your real token> \
  -e DASHBOARD_ADMIN_USERNAME=admin \
  -e DASHBOARD_ADMIN_PASSWORD_HASH='<your real bcrypt hash>' \
  -e ITEMFORGE_REFERENCE_DIR=/data/reference \
  -v itemforge-reference:/data \
  itemforge-dashboard
```

The volume matters: without it the reference texture library lives inside the container and is
wiped every time the container is recreated. (`./install.sh` handles this for you by mounting
`./dashboard-data`.)

## Texture Studio & reference library

### Texture Studio

A pixel editor that runs in the browser, so textures can be drawn and saved onto an item without
a separate image tool. Every texture panel in the item/armor/block forms has a button that opens
it as a modal; the same workspace is also a real page you can bookmark or share:

| URL | Edits |
| --- | --- |
| `/studio?type=item&id=<item id>` | 16×16 item icon |
| `/studio?type=block&id=<block id>` | 16×16 block texture (all six faces) |
| `/studio?type=armor-icon&id=<armor id>` | 16×16 inventory icon of an armor piece |
| `/studio?type=armor-layer&id=<armor id>&layer=humanoid` | 64×32 body layer |
| `/studio?type=armor-layer&id=<armor id>&layer=humanoid_leggings` | 64×32 leggings layer |
| `/studio` | Scratch canvas — not linked to anything, download the PNG yourself |

Armor layers are keyed by `armor-asset-id`, not by armor id: editing the body layer from the
helmet's page changes it for **every piece in that set**, exactly as the resource pack works.
Unsaved work is kept in browser local storage per target, so a reload won't lose your drawing.

Saving goes through the plugin's texture upload API and rebuilds the resource pack, so its size
cap applies — see the two different limits below.

### Reference library

`/reference` lets you import a resource pack (`.zip`) and browse or search its textures, and the
Studio can pull one in as a drawing reference. Only PNG entries matching
`assets/<namespace>/textures/**.png` are extracted, and each one is checked for a real PNG
signature before being written.

**ItemForge bundles no Minecraft assets and downloads none.** Mojang's assets are copyrighted;
this library only ever holds resource packs you imported yourself, stored locally on the machine
running the dashboard.

### Upload limits — two different caps

These are easy to confuse because they apply to different things:

| What | Limit | Set where |
| --- | --- | --- |
| Reference pack `.zip` upload | 128 MB | `spring.servlet.multipart.max-file-size` (dashboard) |
| Saving a texture from the Studio | 2 MiB | `dashboard-api.max-upload-bytes` (plugin `config.yml`) |

The reference pack never reaches the plugin — import is entirely dashboard-local and does not
rebuild the resource pack.

### Reference library settings (`application.yml`)

| Key | Default | Description |
| --- | --- | --- |
| `itemforge.reference.dir` | `<user.home>/.itemforge-dashboard/reference` | Where imported packs are extracted. Override with the `ITEMFORGE_REFERENCE_DIR` env var. |
| `itemforge.reference.max-total-bytes` | `268435456` (256 MiB) | Max bytes read out of one zip **after** decompression. |
| `itemforge.reference.max-entry-bytes` | `4194304` (4 MiB) | Max size of a single entry. |
| `itemforge.reference.max-entries` | `20000` | Max number of textures written from one pack. |

The last three are zip-bomb guards — a small archive that expands to something huge is rejected
mid-extraction rather than filling the disk. Only raise them if a legitimate pack is refused.

## Balance page

`/balance` shows the same report as `/itemforge analyze` in game, grouped by severity, with each
finding labelled by where it came from — a deterministic rule or the AI review. See
[Balance analysis](../README.md#balance-analysis) in the main README for what the rules check.

- The box at the top narrows the report to a single item, armor, or block id; leave it empty for
  the whole config.
- **Re-run** re-runs the analysis and swaps only the results back in, so the page does not reload
  around what can be a slow AI call when `ai.enabled: true`.
- If the plugin is not running or `dashboard-api` is unreachable, the page renders itself with an
  explanation rather than redirecting to an error page — the same behaviour as the Overview.

The report is computed entirely by the plugin; the dashboard only fetches `GET /api/balance` and
renders it. Whether the AI half runs is decided by `ai.enabled` in the plugin's `config.yml`, not
by anything configured here.

## Environment variables

All variables are read via Spring's `${VAR:default}` placeholder syntax
(`dashboard/src/main/resources/application.yml`). None of the defaults below are safe to run
in production — they exist only so a missing variable fails loudly instead of silently.

| Variable | Required | Default | Description |
| --- | --- | --- | --- |
| `ITEMFORGE_API_BASE_URL` | Yes | `http://localhost:8082` | Base URL of the plugin's `dashboard-api` HTTP server. Note the plugin's own default port is `8081` (`dashboard-api.port` in `config.yml`) — always double-check this matches what the plugin is actually listening on, don't rely on the dashboard's built-in default. |
| `ITEMFORGE_API_TOKEN` | Yes | `CHANGE_ME` | Must exactly match the plugin's `dashboard-api.api-key`. Sent as `Authorization: Bearer <token>` on every request. The plugin refuses to start its API server while its side is still `CHANGE_ME`, so this has to be a real value on both ends. |
| `DASHBOARD_ADMIN_USERNAME` | No | `admin` | Login username for the dashboard's own admin account (separate from any Minecraft account). |
| `DASHBOARD_ADMIN_PASSWORD_HASH` | Yes | `CHANGE_ME_SET_A_REAL_BCRYPT_HASH` (not a valid hash — login fails until replaced) | BCrypt hash of the admin login password. Generate with `htpasswd -inBC 10 "" <<< '<password>' \| tr -d ':\n'`, or without `htpasswd` installed via `docker run --rm -i httpd:2.4-alpine htpasswd -inBC 10 "" <<< '<password>' \| tr -d ':\n'`. `$2a$`, `$2b$` and `$2y$` are all accepted. Never put a plaintext password here. |
| `SERVER_PORT` | No | `8090` | Standard Spring Boot port override, if you need the dashboard on a different port. |
| `ITEMFORGE_REFERENCE_DIR` | No | `<user.home>/.itemforge-dashboard/reference` | Where imported reference packs are extracted. In Docker this is set to `/data/reference` by `docker-compose.yml` and backed by the `./dashboard-data` bind mount — leaving it at the default there means losing the library on every container rebuild. |

## License

Same as the main project — see the [LICENSE](../LICENSE) file at the repository root.
