#!/usr/bin/env bash
# Install/start ItemForge through Docker Compose. This is an ALTERNATIVE to the
# plain Java setup (see README.md) -- this script does not build the jar, it
# only packages the deployment. Safe to run repeatedly (idempotent against an
# existing .env and config.yml).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

ENV_FILE=".env"
ENV_EXAMPLE=".env.example"
JAR_PATH="./itemforge.jar"
PLUGIN_CONFIG_PATH="server-data/plugins/ItemForge/config.yml"
READY_TIMEOUT="${READY_TIMEOUT:-180}"
DOCKER_COMPOSE_CMD=()

# Which BCrypt generator resolve_bcrypt_generator() settled on, and the shape a
# valid hash must have. Spring Security accepts $2a$, $2b$ and $2y$ alike.
BCRYPT_METHOD=""
BCRYPT_DOCKER_IMAGE="httpd:2.4-alpine"
BCRYPT_HASH_PATTERN='^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$'
BCRYPT_PROBE_PASSWORD="itemforge-generator-probe"

# Paper's build index, used to reject a mistyped MC_VERSION at the prompt
# instead of letting the container crash-loop on it. The v2 API every older
# script uses has been sunset -- v3 (fill) is the current one.
PAPER_API_URL="https://fill.papermc.io/v3/projects/paper"
PAPER_API_USER_AGENT="ItemForge-install.sh (+https://github.com/hoangluongtran0309/ItemForge)"

# ------------------------------------------------------------------------
# Colors: detect whether the terminal supports ANSI, fall back to plain text
# if it does not (e.g. running in CI, or TERM=dumb, or NO_COLOR is set).
# ------------------------------------------------------------------------
supports_color() {
  [[ -t 1 ]] || return 1
  [[ -z "${NO_COLOR:-}" ]] || return 1
  [[ "${TERM:-}" != "dumb" ]] || return 1
  return 0
}

if supports_color; then
  C_RESET=$'\033[0m'; C_RED=$'\033[31m'; C_GREEN=$'\033[32m'
  C_YELLOW=$'\033[33m'; C_BLUE=$'\033[34m'
else
  C_RESET=""; C_RED=""; C_GREEN=""; C_YELLOW=""; C_BLUE=""
fi

log_info()    { printf '%s[INFO]%s %s\n'  "$C_BLUE"   "$C_RESET" "$*"; }
log_success() { printf '%s[ OK ]%s %s\n'  "$C_GREEN"  "$C_RESET" "$*"; }
log_warn()    { printf '%s[WARN]%s %s\n'  "$C_YELLOW" "$C_RESET" "$*" >&2; }
log_error()   { printf '%s[FAIL]%s %s\n'  "$C_RED"    "$C_RESET" "$*" >&2; }

prompt_default() {
  # prompt_default "Question" "default value" -> prints the chosen value
  local question="$1" default="$2" answer=""
  read -r -p "$question [$default]: " answer || true
  if [[ -z "$answer" ]]; then
    printf '%s' "$default"
  else
    printf '%s' "$answer"
  fi
}

# ------------------------------------------------------------------------
# A run that dies halfway through setup must not leave a partially written
# .env behind: the next run would take the "existing .env" branch, keep that
# broken file and then fail validation, forcing the user to delete it by hand.
# ------------------------------------------------------------------------
ENV_SETUP_IN_PROGRESS=0
ENV_SETUP_BACKUP=""

cleanup_incomplete_env() {
  [[ "$ENV_SETUP_IN_PROGRESS" -eq 1 ]] || return 0
  ENV_SETUP_IN_PROGRESS=0
  rm -f "$ENV_FILE"
  if [[ -n "$ENV_SETUP_BACKUP" && -f "$ENV_SETUP_BACKUP" ]]; then
    mv "$ENV_SETUP_BACKUP" "$ENV_FILE"
    log_warn "Setup did not finish -- restored your previous .env from ${ENV_SETUP_BACKUP}."
  else
    log_warn "Setup did not finish -- removed the incomplete .env so the next run starts clean."
  fi
}

trap cleanup_incomplete_env EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

# ------------------------------------------------------------------------
# Read/write .env WITHOUT using `source` -- a value may contain '$' (a bcrypt
# hash looks like $2y$10$..., for example) which bash would misread as a
# variable expansion if sourced directly. Parse and write with grep/awk
# instead, the same way docker compose reads its own .env file (plain
# KEY=VALUE, no shell eval).
# ------------------------------------------------------------------------
env_get() {
  local key="$1"
  [[ -f "$ENV_FILE" ]] || { printf ''; return 0; }
  local line val
  line="$(grep -E "^${key}=" "$ENV_FILE" | tail -n1)" || true
  if [[ -z "$line" ]]; then
    printf ''
    return 0
  fi
  val="${line#*=}"
  if [[ ${#val} -ge 2 && "${val:0:1}" == "'" && "${val: -1}" == "'" ]]; then
    val="${val:1:${#val}-2}"
  elif [[ ${#val} -ge 2 && "${val:0:1}" == '"' && "${val: -1}" == '"' ]]; then
    val="${val:1:${#val}-2}"
  fi
  printf '%s' "$val"
}

set_env_var() {
  local key="$1" val="$2" escaped_val line
  # Wrap the value in single quotes so $, / and . inside a token or hash are
  # safe; escape any single quote in the content (practically never happens
  # with a hex token or a bcrypt hash, but handle it anyway).
  escaped_val="$(printf '%s' "$val" | sed "s/'/'\\\\''/g")"
  line="${key}='${escaped_val}'"
  if [[ -f "$ENV_FILE" ]] && grep -q "^${key}=" "$ENV_FILE"; then
    awk -v k="$key" -v newline="$line" '{ if ($0 ~ "^" k "=") print newline; else print }' \
      "$ENV_FILE" > "${ENV_FILE}.tmp"
    mv "${ENV_FILE}.tmp" "$ENV_FILE"
  else
    printf '%s\n' "$line" >> "$ENV_FILE"
  fi
}

# ------------------------------------------------------------------------
# BCrypt hash generation.
#
# The dashboard authenticates against a BCrypt hash, and nothing in coreutils
# can produce one -- not even `openssl passwd`, which only offers MD5-crypt,
# apr1 and SHA-crypt. Rather than hard-requiring the `htpasswd` binary (absent
# by default on Fedora and Debian), probe every generator we can reach and
# keep the first one that really produces a valid hash. Docker is already a
# mandatory prerequisite, so the container fallback means a missing
# httpd-tools/apache2-utils package can never block an install.
# ------------------------------------------------------------------------
bcrypt_hash() {
  # Reads the plaintext password on stdin -- never as an argument, so it does
  # not show up in the process list to other local users. Prints the hash.
  case "$BCRYPT_METHOD" in
    htpasswd)
      htpasswd -inBC 10 "" | tr -d ':\n'
      ;;
    docker)
      docker run --rm -i "$BCRYPT_DOCKER_IMAGE" htpasswd -inBC 10 "" | tr -d ':\n'
      ;;
    python3)
      python3 -c 'import bcrypt, sys; pw = sys.stdin.buffer.readline().rstrip(b"\n"); sys.stdout.write(bcrypt.hashpw(pw, bcrypt.gensalt(10)).decode())'
      ;;
    *)
      return 1
      ;;
  esac
}

bcrypt_probe() {
  # Do not just check that a tool exists -- actually hash a throwaway password
  # with it and verify the output, so a broken candidate is skipped instead of
  # trusted and discovered at the password prompt.
  local method="$1" saved="$BCRYPT_METHOD" out=""
  BCRYPT_METHOD="$method"
  out="$(printf '%s\n' "$BCRYPT_PROBE_PASSWORD" | bcrypt_hash 2>/dev/null || true)"
  BCRYPT_METHOD="$saved"
  [[ "$out" =~ $BCRYPT_HASH_PATTERN ]]
}

resolve_bcrypt_generator() {
  local method
  for method in htpasswd docker python3; do
    case "$method" in
      htpasswd)
        command -v htpasswd >/dev/null 2>&1 || continue
        ;;
      docker)
        command -v docker >/dev/null 2>&1 || continue
        if ! docker image inspect "$BCRYPT_DOCKER_IMAGE" >/dev/null 2>&1; then
          log_info "No local 'htpasswd' -- fetching ${BCRYPT_DOCKER_IMAGE} to hash the admin password."
          docker pull -q "$BCRYPT_DOCKER_IMAGE" >/dev/null 2>&1 || continue
        fi
        ;;
      python3)
        command -v python3 >/dev/null 2>&1 || continue
        python3 -c 'import bcrypt' >/dev/null 2>&1 || continue
        ;;
    esac

    if bcrypt_probe "$method"; then
      BCRYPT_METHOD="$method"
      return 0
    fi
    log_warn "BCrypt generator '${method}' is present but did not return a valid hash -- trying the next one."
  done
  return 1
}

print_bcrypt_install_hint() {
  log_error "No way to generate a BCrypt hash for the admin password was found."
  log_error "Any one of these is enough -- install it and re-run ./install.sh:"
  log_error "  Fedora/RHEL:    sudo dnf install httpd-tools"
  log_error "  Debian/Ubuntu:  sudo apt-get install apache2-utils"
  log_error "  macOS:          htpasswd ships with the system Apache"
  log_error "  Any OS:         pip install bcrypt"
  log_error "Otherwise give Docker network access so it can pull ${BCRYPT_DOCKER_IMAGE}."
}

# ------------------------------------------------------------------------
# (a) Check prerequisites -- do NOTHING else until all of these pass.
# ------------------------------------------------------------------------
print_docker_install_hint() {
  if [[ -f /etc/os-release ]]; then
    local os_id
    os_id="$(. /etc/os-release && echo "$ID")"
    case "$os_id" in
      fedora|rhel|centos|rocky|almalinux)
        log_info "Install Docker (Fedora/RHEL/CentOS):"
        log_info "  sudo dnf -y install dnf-plugins-core"
        log_info "  sudo dnf config-manager --add-repo https://download.docker.com/linux/fedora/docker-ce.repo"
        log_info "  sudo dnf install docker-ce docker-ce-cli containerd.io docker-compose-plugin"
        log_info "  sudo systemctl enable --now docker"
        return
        ;;
      ubuntu|debian)
        log_info "Install Docker (Debian/Ubuntu):"
        log_info "  See https://docs.docker.com/engine/install/ubuntu/ (or /debian/)"
        log_info "  -- it walks you through adding the Docker apt repository, then"
        log_info "     installing docker-ce plus the compose plugin."
        return
        ;;
    esac
  fi
  if [[ "$(uname -s)" == "Darwin" ]]; then
    log_info "Install Docker Desktop for macOS: https://docs.docker.com/desktop/install/mac-install/"
    return
  fi
  log_info "See https://docs.docker.com/engine/install/ for instructions for your OS."
}

check_prerequisites() {
  log_info "Checking prerequisites..."
  local missing=0

  if ! command -v docker >/dev/null 2>&1; then
    log_error "'docker' was not found in PATH."
    print_docker_install_hint
    missing=1
  fi

  if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    DOCKER_COMPOSE_CMD=(docker compose)
  elif command -v docker-compose >/dev/null 2>&1; then
    DOCKER_COMPOSE_CMD=(docker-compose)
  else
    log_error "Neither 'docker compose' nor 'docker-compose' was found in PATH."
    print_docker_install_hint
    missing=1
  fi

  if ! command -v openssl >/dev/null 2>&1; then
    log_error "'openssl' is required to generate a secure ITEMFORGE_API_TOKEN but was not found."
    log_error "  Fedora/RHEL:    sudo dnf install openssl"
    log_error "  Debian/Ubuntu:  sudo apt-get install openssl"
    missing=1
  fi

  # Resolve the password hasher here rather than at the password prompt: it is
  # the last step of the wizard, so failing there throws away every answer the
  # user just typed.
  if [[ "$missing" -eq 0 ]]; then
    if resolve_bcrypt_generator; then
      log_success "Found docker and docker compose (using: ${DOCKER_COMPOSE_CMD[*]}); BCrypt hashes via '${BCRYPT_METHOD}'."
    else
      print_bcrypt_install_hint
      missing=1
    fi
  fi

  if [[ "$missing" -ne 0 ]]; then
    exit 1
  fi
}

ensure_jar() {
  log_info "Checking for itemforge.jar..."
  if [[ -f "$JAR_PATH" ]]; then
    log_success "$JAR_PATH is already in place."
    return
  fi

  shopt -s nullglob
  local candidates=(target/itemforge-*.jar)
  shopt -u nullglob

  if [[ ${#candidates[@]} -eq 0 ]]; then
    log_error "No itemforge.jar in the repo root, and no target/itemforge-*.jar either."
    log_error "Run 'mvn clean package' first, then re-run ./install.sh."
    exit 1
  fi

  # Old builds may still be sitting in target/ -- pick the newest by mtime
  # (never guess from the filename) so we always use the most recent build.
  local newest
  newest="$(ls -t "${candidates[@]}" | head -n1)"
  cp "$newest" "$JAR_PATH"
  log_success "Found $newest -- copied to $JAR_PATH for Docker to mount."
}

ensure_gitignore_entries() {
  local entries=(".env" "/itemforge.jar" "/server-data/" "/dashboard-data/") entry added=0
  [[ -f .gitignore ]] || touch .gitignore
  for entry in "${entries[@]}"; do
    if ! grep -qxF "$entry" .gitignore; then
      printf '%s\n' "$entry" >> .gitignore
      added=1
    fi
  done
  [[ "$added" -eq 0 ]] || log_success "Added the missing entries to .gitignore."
}

# ------------------------------------------------------------------------
# (b)/(c) Set up .env
# ------------------------------------------------------------------------
# 0 = Paper publishes this version, 1 = it does not, 2 = the check could not
# run (no curl, offline, API down). A version typed on a machine that cannot
# reach the API must still be accepted -- an installer that refuses to work
# offline is worse than one that lets a typo through.
paper_version_exists() {
  local version="$1" code
  command -v curl >/dev/null 2>&1 || return 2
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 \
          -A "$PAPER_API_USER_AGENT" "${PAPER_API_URL}/versions/${version}")" || return 2
  case "$code" in
    200) return 0 ;;
    404) return 1 ;;
    *)   return 2 ;;
  esac
}

prompt_mc_version() {
  local version rc
  while true; do
    version="$(prompt_default "Minecraft/Paper version (the VERSION var of itzg/minecraft-server)" "LATEST")"

    # itzg/minecraft-server resolves these itself; they are not entries in
    # Paper's version list, so asking the API about them would always 404.
    if [[ "$version" == "LATEST" || "$version" == "SNAPSHOT" ]]; then
      break
    fi

    rc=0
    paper_version_exists "$version" || rc=$?
    if (( rc == 0 )); then
      break
    fi
    if (( rc == 2 )); then
      log_warn "Could not reach the Paper API to verify '$version' -- keeping it as typed."
      break
    fi

    log_warn "Paper has no build for '$version', so the server container would fail to start."
    log_warn "Type a real Minecraft version (e.g. 1.21.4) or 'LATEST'. The full list is at"
    log_warn "  ${PAPER_API_URL}"
  done
  printf '%s' "$version"
}

check_port_collisions() {
  # Print the error and return 1 (rather than exiting directly) so this
  # function works both when called plainly (set -e stops the script) and
  # when called as part of an `||` condition.
  local -a ports=("$@")
  local -A seen=()
  local p
  for p in "${ports[@]}"; do
    if [[ -n "${seen[$p]:-}" ]]; then
      log_error "Port $p is used for more than one purpose (resource-pack / dashboard-api / dashboard web must all differ)."
      return 1
    fi
    seen[$p]=1
  done
  return 0
}

confirm_eula() {
  echo
  log_warn "Minecraft/Paper requires you to accept the Mojang EULA to run: https://www.minecraft.net/en-us/eula"
  local answer=""
  read -r -p "Type 'yes' to accept the EULA and continue: " answer || true
  if [[ "$answer" != "yes" ]]; then
    log_error "EULA not accepted -- aborting."
    exit 1
  fi
  set_env_var EULA "true"
}

generate_admin_password_hash() {
  local password="" password_confirm=""
  while true; do
    # A closed stdin must abort rather than loop: `read || true` would hand
    # back an empty password forever and spin here.
    if ! read -r -s -p "Dashboard admin password: " password; then
      echo
      log_error "Input stream closed before a password was entered -- aborting."
      exit 1
    fi
    echo
    if ! read -r -s -p "Confirm the password: " password_confirm; then
      echo
      log_error "Input stream closed before the password was confirmed -- aborting."
      exit 1
    fi
    echo
    if [[ -z "$password" ]]; then
      log_warn "The password cannot be empty. Try again."
      continue
    fi
    if [[ "$password" != "$password_confirm" ]]; then
      log_warn "The two entries do not match. Try again."
      continue
    fi
    break
  done

  local hash
  hash="$(printf '%s\n' "$password" | bcrypt_hash 2>/dev/null || true)"
  unset password password_confirm

  if [[ ! "$hash" =~ $BCRYPT_HASH_PATTERN ]]; then
    unset hash
    log_error "The BCrypt generator ('${BCRYPT_METHOD}') returned a malformed hash."
    log_error "Nothing was saved -- re-run ./install.sh."
    exit 1
  fi

  set_env_var DASHBOARD_ADMIN_PASSWORD_HASH "$hash"
  unset hash
  log_success "Admin password hashed and stored in .env (the plaintext is never written)."
}

first_run_setup() {
  log_info "No .env yet -- setting up the ItemForge Docker deployment."
  ENV_SETUP_IN_PROGRESS=1
  cp "$ENV_EXAMPLE" "$ENV_FILE"

  local mc_version
  mc_version="$(prompt_mc_version)"
  set_env_var MC_VERSION "$mc_version"

  confirm_eula

  local mc_port resource_pack_port dashboard_api_port dashboard_port
  mc_port="$(prompt_default "Minecraft port (published to the host)" "25565")"
  resource_pack_port="$(prompt_default "Resource-pack HTTP port (published to the host)" "8080")"
  dashboard_api_port="$(prompt_default "Internal API port between dashboard and plugin (NOT published)" "8081")"
  dashboard_port="$(prompt_default "Dashboard web port (published to the host)" "8090")"

  check_port_collisions "$resource_pack_port" "$dashboard_api_port" "$dashboard_port"

  set_env_var MC_PORT "$mc_port"
  set_env_var RESOURCE_PACK_PORT "$resource_pack_port"
  set_env_var DASHBOARD_API_PORT "$dashboard_api_port"
  set_env_var DASHBOARD_PORT "$dashboard_port"

  local resource_pack_host
  resource_pack_host="$(prompt_default "Public IP/domain clients download the resource pack from (leave blank to disable for now)" "")"
  set_env_var RESOURCE_PACK_HOST "$resource_pack_host"
  if [[ -z "$resource_pack_host" ]]; then
    log_warn "RESOURCE_PACK_HOST is blank -- the plugin will NOT start the resource-pack HTTP server until you set a real host and restart."
  fi

  log_info "Generating ITEMFORGE_API_TOKEN..."
  local api_token
  api_token="$(openssl rand -hex 32)"
  set_env_var ITEMFORGE_API_TOKEN "$api_token"
  unset api_token
  log_success "Generated ITEMFORGE_API_TOKEN and saved it to .env."

  local admin_username
  admin_username="$(prompt_default "Dashboard admin username" "admin")"
  set_env_var DASHBOARD_ADMIN_USERNAME "$admin_username"

  generate_admin_password_hash

  ENV_SETUP_IN_PROGRESS=0
  ENV_SETUP_BACKUP=""
}

existing_run_confirm() {
  log_info ".env already exists."
  local answer=""
  read -r -p "Keep the current configuration? [Y/n]: " answer || true
  answer="${answer:-Y}"
  case "$answer" in
    [Yy]*)
      log_info "Keeping .env as-is, nothing changed."
      ;;
    *)
      local backup=".env.bak.$(date +%Y%m%d%H%M%S)"
      cp "$ENV_FILE" "$backup"
      log_success "Backed up the old .env to $backup."
      # Hand the backup to the cleanup trap so an aborted reconfigure restores
      # the working config instead of leaving the user with nothing.
      ENV_SETUP_BACKUP="$backup"
      rm -f "$ENV_FILE"
      first_run_setup
      ;;
  esac
}

validate_env() {
  # Neither the plugin nor the dashboard fails fast at boot when config is
  # missing -- install.sh is the last line of defence before the containers
  # actually run.
  log_info "Validating .env..."
  local token hash eula pack_port api_port dash_port failed=0

  token="$(env_get ITEMFORGE_API_TOKEN)"
  hash="$(env_get DASHBOARD_ADMIN_PASSWORD_HASH)"
  eula="$(env_get EULA)"
  pack_port="$(env_get RESOURCE_PACK_PORT)"
  api_port="$(env_get DASHBOARD_API_PORT)"
  dash_port="$(env_get DASHBOARD_PORT)"

  if [[ -z "$token" ]]; then
    log_error "ITEMFORGE_API_TOKEN is empty in .env. Delete .env and re-run ./install.sh to generate a new one."
    failed=1
  fi
  if [[ -z "$hash" || "$hash" == "CHANGE_ME_SET_A_REAL_BCRYPT_HASH" ]]; then
    log_error "DASHBOARD_ADMIN_PASSWORD_HASH is empty or still the placeholder. Delete .env and re-run ./install.sh."
    failed=1
  fi
  if [[ "$eula" != "true" ]]; then
    log_error "The EULA has not been accepted (must be 'true'). Delete .env and re-run ./install.sh."
    failed=1
  fi
  if [[ -n "$pack_port" && -n "$api_port" && -n "$dash_port" ]]; then
    check_port_collisions "$pack_port" "$api_port" "$dash_port" || failed=1
  fi

  if [[ "$failed" -ne 0 ]]; then
    log_error "Configuration is not valid -- refusing to start the containers with missing or unsafe settings."
    exit 1
  fi
  log_success "Configuration is valid."
}

# ------------------------------------------------------------------------
# The plugin does not read environment variables -- everything comes from
# config.yml, and Bukkit's saveDefaultConfig() only writes the defaults IF the
# file does not exist yet. So for dashboard-api.enabled/port/api-key and
# resource-pack.host/port to hold real values, this config.yml has to be
# written BEFORE the first `docker compose up`. If the file already exists (a
# later run), leave it alone.
# ------------------------------------------------------------------------
ensure_plugin_config() {
  if [[ -f "$PLUGIN_CONFIG_PATH" ]]; then
    log_info "$PLUGIN_CONFIG_PATH already exists -- keeping it, not overwriting."
    return
  fi

  log_info "Generating $PLUGIN_CONFIG_PATH so the plugin and dashboard can talk on the very first run..."
  mkdir -p "$(dirname "$PLUGIN_CONFIG_PATH")"

  local resource_pack_host resource_pack_port dashboard_api_port api_token
  resource_pack_host="$(env_get RESOURCE_PACK_HOST)"
  resource_pack_port="$(env_get RESOURCE_PACK_PORT)"
  dashboard_api_port="$(env_get DASHBOARD_API_PORT)"
  api_token="$(env_get ITEMFORGE_API_TOKEN)"
  [[ -n "$resource_pack_host" ]] || resource_pack_host="CHANGE_ME"

  cat > "$PLUGIN_CONFIG_PATH" <<EOF
resource-pack:
  # Public address Minecraft clients use to download the resource pack.
  # Filled in from RESOURCE_PACK_HOST in .env by install.sh.
  host: "${resource_pack_host}"
  port: ${resource_pack_port}

hud:
  bossbar-update-interval-ticks: 5

ai:
  # Out of scope for install.sh -- put a real api-key here (or set it through
  # the dashboard) if you want to use the AI generate feature.
  enabled: false
  provider: "claude"
  claude:
    api-key: "CHANGE_ME"
    model: "claude-haiku-4-5"
    max-tokens: 2048
    timeout-seconds: 30
  chatgpt:
    api-key: "CHANGE_ME"
    model: "gpt-4o-mini"
    max-tokens: 2048
    timeout-seconds: 30
  deepseek:
    api-key: "CHANGE_ME"
    model: "deepseek-chat"
    max-tokens: 2048
    timeout-seconds: 30
  gemini:
    api-key: "CHANGE_ME"
    model: "gemini-2.0-flash"
    max-tokens: 2048
    timeout-seconds: 30

dashboard-api:
  # Enabled and filled in by install.sh from DASHBOARD_API_PORT /
  # ITEMFORGE_API_TOKEN in .env, so the dashboard container can reach the
  # plugin over the internal Docker network.
  enabled: true
  port: ${dashboard_api_port}
  api-key: "${api_token}"
  max-upload-bytes: 2097152
EOF

  log_success "Created $PLUGIN_CONFIG_PATH."
  if [[ "$resource_pack_host" == "CHANGE_ME" ]]; then
    log_warn "resource-pack.host is still CHANGE_ME -- the plugin will not start the resource-pack HTTP server until you set a real host in $PLUGIN_CONFIG_PATH and restart the paper-server container."
  fi
}

# ------------------------------------------------------------------------
# Create the dashboard's bind-mount directory up front. If Docker creates it
# during `up`, it ends up owned by root and an admin without sudo cannot
# delete or back it up.
# ------------------------------------------------------------------------
ensure_dashboard_data_dir() {
  if [[ ! -d dashboard-data ]]; then
    mkdir -p dashboard-data
    log_success "Created ./dashboard-data (the dashboard's reference texture library)."
  fi
}

# ------------------------------------------------------------------------
# (d) Start the containers
# ------------------------------------------------------------------------
compose_up() {
  log_info "Starting the containers (${DOCKER_COMPOSE_CMD[*]} up -d --build)..."
  if ! "${DOCKER_COMPOSE_CMD[@]}" up -d --build; then
    log_error "docker compose up failed. See the output above for details."
    exit 1
  fi
  log_success "Containers are up."
}

# ------------------------------------------------------------------------
# (e) Wait and report status
# ------------------------------------------------------------------------
# Prints "<status> <restart-count>" for a compose service, or nothing when the
# container cannot be inspected.
compose_service_state() {
  local service="$1" cid
  cid="$("${DOCKER_COMPOSE_CMD[@]}" ps -q "$service" 2>/dev/null | head -n1)"
  [[ -n "$cid" ]] || return 1
  docker inspect -f '{{.State.Status}} {{.RestartCount}}' "$cid" 2>/dev/null
}

# Paper's own startup line is the only honest readiness signal (see
# wait_for_ready). --tail keeps the grep cheap on a stack that has been running
# for a while; on such a stack the line may have scrolled past it, in which case
# this times out into a warning rather than a false failure.
paper_logs() {
  "${DOCKER_COMPOSE_CMD[@]}" logs --no-color --tail 2000 paper-server 2>/dev/null || true
}

wait_for_ready() {
  local mc_port mc_version waited=0 state status restarts logs baseline_restarts=-1
  mc_port="$(env_get MC_PORT)"
  mc_version="$(env_get MC_VERSION)"
  log_info "Waiting for Paper to finish starting (up to ${READY_TIMEOUT}s)..."

  while (( waited < READY_TIMEOUT )); do
    logs="$(paper_logs)"
    state="$(compose_service_state paper-server || true)"
    status="${state%% *}"
    restarts="${state##* }"
    [[ "$restarts" =~ ^[0-9]+$ ]] || restarts=0
    # Only restarts that happen while we watch mean anything: a container that
    # has been up for weeks can carry a non-zero count from an unrelated event.
    (( baseline_restarts >= 0 )) || baseline_restarts="$restarts"

    # Failure is checked before success on purpose -- a container that is
    # crash-looping right now can still have a stale "Done (...)" line in its
    # log from an earlier, healthy run.
    if grep -qE 'Failed to download (paper|server)|Requested version .* is not available' <<<"$logs"; then
      log_error "paper-server could not download Paper ${mc_version}."
      log_error "MC_VERSION in $ENV_FILE is not a version Paper publishes a build for."
      log_error "Set it to a real version (e.g. 1.21.4) or \"LATEST\" -- the list is at"
      log_error "  ${PAPER_API_URL}"
      log_error "then re-run: ${DOCKER_COMPOSE_CMD[*]} up -d"
      exit 1
    fi

    if [[ "$status" == "restarting" || "$status" == "exited" || "$status" == "dead" ]] \
       || (( restarts > baseline_restarts + 1 )); then
      log_error "The paper-server container is not starting up cleanly (state: ${status:-unknown}, ${restarts} restart(s))."
      log_error "See what it is failing on with: ${DOCKER_COMPOSE_CMD[*]} logs paper-server"
      log_error "On SELinux hosts (Fedora/RHEL/CentOS) a 'Permission denied' on /data means the bind"
      log_error "mounts were not relabelled -- docker-compose.yml uses ':z' for that, so make sure you"
      log_error "are running the current file and re-run: ${DOCKER_COMPOSE_CMD[*]} up -d"
      exit 1
    fi

    # An open TCP port is NOT readiness. Docker publishes the port the moment
    # the container is created, so docker-proxy accepts connections while Paper
    # is still downloading -- or while it is crash-looping and never starts at
    # all. That is why this waits for Paper to say "Done (...)" itself.
    if grep -qE 'Done \([0-9.]+s\)!' <<<"$logs"; then
      log_success "Paper finished starting -- Minecraft is accepting connections on port ${mc_port}."
      return
    fi

    sleep 5
    waited=$((waited + 5))
  done

  log_warn "Paper had still not finished starting after ${READY_TIMEOUT}s. The first run can take longer while Paper downloads and the world generates."
  log_warn "Follow the progress with: ${DOCKER_COMPOSE_CMD[*]} logs -f paper-server"
}

print_summary() {
  local dashboard_port mc_port resource_pack_host
  dashboard_port="$(env_get DASHBOARD_PORT)"
  mc_port="$(env_get MC_PORT)"
  resource_pack_host="$(env_get RESOURCE_PACK_HOST)"

  echo
  log_success "The ItemForge Docker stack is ready."
  echo
  echo "  Dashboard : http://localhost:${dashboard_port}"
  echo "  Minecraft : ${resource_pack_host:-<your-server-ip>}:${mc_port}"
  echo
  log_info "Two directories hold everything worth backing up: ./server-data (world"
  log_info "plus plugin config) and ./dashboard-data (the reference textures you import)."
  log_warn "SECURITY: the dashboard has no built-in HTTPS or reverse proxy. If you"
  log_warn "expose it beyond localhost/LAN you MUST put a reverse proxy with TLS in"
  log_warn "front of it (nginx, Caddy) -- never expose the dashboard directly to the"
  log_warn "public internet."
  echo
  log_info "AI generation is disabled by default. To turn it on, edit $PLUGIN_CONFIG_PATH"
  log_info "(ai.enabled / ai.<provider>.api-key) and restart the paper-server container."
}

main() {
  log_info "ItemForge -- Docker deployment installer"
  check_prerequisites
  ensure_jar
  ensure_gitignore_entries

  if [[ -f "$ENV_FILE" ]]; then
    existing_run_confirm
  else
    first_run_setup
  fi

  validate_env
  ensure_plugin_config
  ensure_dashboard_data_dir
  compose_up
  wait_for_ready
  print_summary
}

main "$@"
