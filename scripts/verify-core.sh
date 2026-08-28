#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd "$script_dir/.." && pwd)"
core="$project_dir/app/src/main/jniLibs/arm64-v8a/libdingooemu.so"

if [[ ! -f "$core" ]]; then
  echo "Missing DingooEmu core: $core" >&2
  exit 1
fi

file "$core" | grep -F "ARM aarch64" >/dev/null

core_strings="$(strings "$core")"
if ! grep -F "save-state decoded payload is" <<<"$core_strings" >/dev/null; then
  echo "The core does not contain the current decoded-state diagnostic." >&2
  echo "Rebuild it from native/DingooEmu before packaging the app." >&2
  exit 1
fi
if grep -F "save-state payload exceeds the decoded size limit" <<<"$core_strings" >/dev/null; then
  echo "The core still contains the obsolete 64 MiB decoded-state limit diagnostic." >&2
  exit 1
fi

if [[ -n "${ANDROID_NDK_HOME:-}" ]]; then
  readelf="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf"
else
  readelf="$(command -v llvm-readelf || command -v readelf || true)"
fi
if [[ -z "$readelf" || ! -x "$readelf" ]]; then
  echo "llvm-readelf/readelf is required to verify ELF alignment." >&2
  exit 1
fi

if ! "$readelf" -lW "$core" | awk '
  $1 == "LOAD" { found = 1; if ($NF != "0x4000") bad = 1 }
  END { exit !(found && !bad) }
'; then
  echo "The DingooEmu core LOAD segments are not all aligned to 16 KB." >&2
  exit 1
fi

echo "Verified current ARM64 DingooEmu core with 16 KB LOAD alignment."
