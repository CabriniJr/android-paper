#!/usr/bin/env bash
# fallout-nx pipeline: from your own Xbox 360 disc of Fallout 3 / New Vegas to recompiled C++ (and, later, a Switch NRO).
#
#   ./pipeline.sh doctor                 check the tools on this machine
#   ./pipeline.sh deps                   fetch the pinned toolchain (deps.lock) into work/
#   ./pipeline.sh sdk                    build the host code generator (rexglue) from the SDK fork
#   ./pipeline.sh extract  <game> <iso>  extract your disc image into work/assets/<game>/game_root
#   ./pipeline.sh identify <game>        hash default.xex and check it against games/<game>/versions.toml
#   ./pipeline.sh codegen  <game>        translate default.xex to C++ (work/<game>/generated)
#   ./pipeline.sh audit    <game>        sweep the code the generator missed (gaps, jump tables)
#   ./pipeline.sh status   [game]        what each stage has produced so far
#   ./pipeline.sh all      <game> <iso>  deps, sdk, extract, identify, codegen, audit
#
# <game> is fo3 or fnv. Everything made from the game (extracted files, generated C++, image dumps, logs) goes to
# work/, which git ignores: nothing derived from the game is ever committed.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
WORK="${FNX_WORK:-$ROOT/work}"
LOCK="$ROOT/deps.lock"
SA2="$WORK/sa2-nx"
JOBS="${FNX_JOBS:-$(nproc 2>/dev/null || sysctl -n hw.ncpu)}"

if [[ -t 2 ]]; then C1=$'\033[1;36m' C2=$'\033[1;33m' C3=$'\033[1;31m' C0=$'\033[0m'; else C1='' C2='' C3='' C0=''; fi
say()  { printf '%s==%s %s\n' "$C1" "$C0" "$*"; }
warn() { printf '%swarn:%s %s\n' "$C2" "$C0" "$*" >&2; }
die()  { printf '%serror:%s %s\n' "$C3" "$C0" "$*" >&2; exit 1; }

need_game() {
  [[ -n "${1:-}" ]] || die "missing <game> (fo3 or fnv)"
  [[ -f "$ROOT/games/$1/versions.toml" ]] || die "unknown game '$1' (fo3 or fnv)"
}

host_dir() {
  case "$(uname -s)-$(uname -m)" in
    Darwin-arm64) echo mac-arm64 ;; Darwin-*) echo mac-amd64 ;;
    Linux-aarch64) echo linux-arm64 ;; *) echo linux-amd64 ;;
  esac
}
rexglue_bin() { echo "${REXGLUE:-$SA2/sdk/out/$(host_dir)/rexglue}"; }

# --- doctor -------------------------------------------------------------------------------------------------------
version_ge() { [[ "$(printf '%s\n%s\n' "$2" "$1" | sort -V | head -1)" == "$2" ]]; }

check() {  # check <label> <required|optional> <command> [min version]
  local label="$1" kind="$2" cmd="$3" min="${4:-}" ver
  if ! command -v "$cmd" >/dev/null 2>&1; then
    [[ "$kind" == required ]] && { printf '  [ ] %-10s missing (required)\n' "$label"; return 1; }
    printf '  [-] %-10s missing (needed later)\n' "$label"; return 0
  fi
  ver="$("$cmd" --version 2>&1 | grep -oE '[0-9]+\.[0-9]+(\.[0-9]+)?' | head -1)"
  if [[ -n "$min" ]] && ! version_ge "$ver" "$min"; then
    printf '  [!] %-10s %s, need %s+\n' "$label" "$ver" "$min"; [[ "$kind" == required ]] && return 1; return 0
  fi
  printf '  [x] %-10s %s\n' "$label" "$ver"
}

cmd_doctor() {
  local ok=0
  say "host tools (code generator, PC build)"
  check git required git || ok=1
  check cmake required cmake 3.25 || ok=1
  check ninja required ninja || ok=1
  check clang required "${CC:-clang}" 20 || ok=1
  check python3 required python3 3.11 || ok=1
  if python3 -c 'import xxhash' 2>/dev/null; then echo '  [x] xxhash     python module'
  else echo '  [-] xxhash     python module missing (pip install xxhash; needed by the asset pack)'; fi
  say "Switch build (needed from milestone 4)"
  if [[ -x "${DEVKITPRO:-/opt/devkitpro}/devkitA64/bin/aarch64-none-elf-gcc" ]]; then
    echo "  [x] devkitA64  ${DEVKITPRO:-/opt/devkitpro}"
  else
    echo '  [-] devkitA64  missing (https://devkitpro.org: devkitA64, libnx, switch-tools)'
  fi
  check node optional node 20
  check dxc optional dxc
  [[ $ok == 0 ]] && say "ready for: deps, sdk, extract, identify, codegen, audit" \
                 || die "install the missing required tools first (clang 20: https://apt.llvm.org)"
}

# --- deps ---------------------------------------------------------------------------------------------------------
cmd_deps() {
  mkdir -p "$WORK"
  local name url rev dest
  while read -r name url rev _; do
    [[ -z "$name" || "$name" == \#* ]] && continue
    dest="$WORK/$name"
    if [[ ! -d "$dest/.git" ]]; then
      say "clone $name @ ${rev:0:12}"
      git init -q "$dest"
      git -C "$dest" remote add origin "$url"
    fi
    if [[ "$(git -C "$dest" rev-parse HEAD 2>/dev/null)" != "$rev" ]]; then
      say "fetch $name @ ${rev:0:12}"
      git -C "$dest" fetch -q --depth 1 origin "$rev"
      git -C "$dest" checkout -q --detach FETCH_HEAD
    else
      say "$name @ ${rev:0:12} (up to date)"
    fi
  done < "$LOCK"
  # The SDK fork only carries the third-party files it changed; the rest comes from ReXGlue v0.10.0's submodules.
  if [[ ! -f "$SA2/sdk/thirdparty/.fnx_fetched" ]]; then
    say "fetch the SDK's third-party sources (ReXGlue v0.10.0 submodules)"
    python3 "$SA2/tools/fetch_thirdparty.py"
    touch "$SA2/sdk/thirdparty/.fnx_fetched"
  fi
}

# --- sdk ----------------------------------------------------------------------------------------------------------
cmd_sdk() {
  [[ -d "$SA2/sdk" ]] || die "run ./pipeline.sh deps first"
  local out="$SA2/sdk/out/host" log="$WORK/logs/sdk_build.log"
  mkdir -p "$WORK/logs"
  say "configure the code generator ($out)"
  cmake -S "$SA2/sdk" -B "$out" -G Ninja -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_C_COMPILER="${CC:-clang}" -DCMAKE_CXX_COMPILER="${CXX:-clang++}" > "$log" 2>&1 \
    || { tail -30 "$log"; die "configure failed, see $log"; }
  say "build rexglue ($JOBS jobs, log: $log)"
  cmake --build "$out" --target rexglue -j "$JOBS" >> "$log" 2>&1 || { tail -30 "$log"; die "build failed, see $log"; }
  local bin; bin="$(find "$SA2/sdk/out" -type f -name rexglue -perm -u+x | head -1)"
  [[ -n "$bin" ]] || die "rexglue was not produced, see $log"
  mkdir -p "$(dirname "$(rexglue_bin)")"
  [[ "$bin" == "$(rexglue_bin)" ]] || ln -sf "$bin" "$(rexglue_bin)"
  say "code generator: $(rexglue_bin)"
}

# --- extract / identify -------------------------------------------------------------------------------------------
cmd_extract() {
  need_game "${1:-}"; local game="$1" iso="${2:-}"
  [[ -f "$iso" ]] || die "usage: ./pipeline.sh extract $game <your disc .iso>"
  [[ -f "$SA2/tools/extract_iso.py" ]] || die "run ./pipeline.sh deps first"
  local dest="$WORK/assets/$game/game_root"
  mkdir -p "$dest"
  say "extract $(basename "$iso") -> $dest"
  python3 "$SA2/tools/extract_iso.py" "$iso" -o "$dest"
}

cmd_identify() {
  need_game "${1:-}"; local game="$1" xex="$WORK/assets/$1/game_root/default.xex"
  [[ -f "$xex" ]] || die "$xex not found; run ./pipeline.sh extract $game <iso> first"
  python3 "$ROOT/tools/identify.py" "$game" "$xex" --write "$WORK/$game/identity.txt"
}

# --- codegen ------------------------------------------------------------------------------------------------------
cmd_codegen() {
  need_game "${1:-}"; local game="$1" rex; rex="$(rexglue_bin)"
  [[ -x "$rex" ]] || die "code generator not built; run ./pipeline.sh sdk"
  [[ -f "$WORK/assets/$game/game_root/default.xex" ]] || die "run ./pipeline.sh extract $game <iso> first"
  [[ -f "$WORK/$game/identity.txt" ]] || cmd_identify "$game"
  mkdir -p "$WORK/$game" "$WORK/logs"
  local log="$WORK/logs/${game}_codegen.log"
  say "codegen $game (log: $log)"
  # REXGLUE_DUMP_IMAGE: the loaded (decrypted, decompressed) image, read by the audit tools and the post-codegen steps.
  set +e
  (cd "$ROOT/games/$game" && REX_MAX_JUMP_TABLE_ENTRIES=1024 REXGLUE_DUMP_IMAGE="$WORK/$game/default_image.bin" \
     "$rex" codegen "${game}_manifest.toml" ${FNX_CODEGEN_FLAGS:-}) > "$log" 2>&1
  local code=$?
  set -e
  tail -15 "$log"
  if [[ $code != 0 ]]; then
    warn "codegen exit $code. Unresolved functions are expected on a first pass: read $log, add each fix to"
    warn "games/$game/overrides.toml with its evidence, or run FNX_CODEGEN_FLAGS=--force ./pipeline.sh codegen $game"
    return $code
  fi
}

# --- audit --------------------------------------------------------------------------------------------------------
cmd_audit() {
  need_game "${1:-}"; local game="$1" gen="$WORK/$1/generated/default" out="$WORK/$1/audit"
  [[ -d "$gen" ]] || die "no generated code; run ./pipeline.sh codegen $game"
  mkdir -p "$out"
  say "gaps: code between functions that nothing reaches statically"
  python3 "$SA2/tools/find_gaps.py" --gen "$gen" --report "$out/gaps.txt" --toml "$out/gap_candidates.toml" | tail -5
  # switch_check needs the code range of this executable (lo:hi, hex). It is printed by the codegen log; set it in
  # games/<game>/versions.toml as code_range once known.
  local range; range="$(python3 -c "import tomllib,sys;print(tomllib.load(open(sys.argv[1],'rb')).get('code_range',''))" \
                        "$ROOT/games/$game/versions.toml")"
  if [[ -n "$range" && -f "$WORK/$game/default_image.bin" ]]; then
    say "jump tables: every bctr against the generated switch statements"
    python3 "$SA2/tools/switch_check.py" --image "$WORK/$game/default_image.bin" --gen "$gen" --code "$range" \
      --toml "$out/switch_tables.toml" | tail -5
  else
    warn "skipping the jump table check: set code_range in games/$game/versions.toml (see the codegen log)"
  fi
  say "reports in $out: review them, then move each confirmed fix to games/$game/overrides.toml with its evidence"
}

# --- status -------------------------------------------------------------------------------------------------------
cmd_status() {
  local games=("${1:-fo3}"); [[ -z "${1:-}" ]] && games=(fo3 fnv)
  mark() { [[ -e "$1" ]] && echo "[x]" || echo "[ ]"; }
  echo "$(mark "$SA2/.git") deps      $SA2"
  echo "$(mark "$(rexglue_bin)") sdk       $(rexglue_bin)"
  for g in "${games[@]}"; do
    echo "-- $g"
    echo "$(mark "$WORK/assets/$g/game_root/default.xex") extract   work/assets/$g/game_root"
    echo "$(mark "$WORK/$g/identity.txt") identify  work/$g/identity.txt"
    echo "$(mark "$WORK/$g/generated/default") codegen   work/$g/generated"
    echo "$(mark "$WORK/$g/audit/gaps.txt") audit     work/$g/audit"
  done
}

cmd_all() {
  need_game "${1:-}"
  cmd_deps; cmd_sdk; cmd_extract "$1" "${2:-}"; cmd_identify "$1"; cmd_codegen "$1"; cmd_audit "$1"
}

stage="${1:-}"; shift || true
case "$stage" in
  doctor|deps|sdk|extract|identify|codegen|audit|status|all) "cmd_$stage" "$@" ;;
  *) sed -n '2,15p' "$0" | sed 's/^# \{0,1\}//'; exit 1 ;;
esac
