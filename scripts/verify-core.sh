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

expected_revision="$(tr -d '\r\n' < "$project_dir/native/UPSTREAM_COMMIT")"
actual_revision="$(git -C "$project_dir/native/DingooEmu" rev-parse HEAD)"
if [[ "$actual_revision" != "$expected_revision" ]]; then
  echo "Core submodule does not match native/UPSTREAM_COMMIT." >&2
  exit 1
fi
if ! git -C "$project_dir/native/DingooEmu" diff --quiet HEAD; then
  echo "The official core checkout contains local source changes." >&2
  exit 1
fi
expected_hash="$(tr -d '\r\n' < "$project_dir/native/core-build.sha256")"
actual_hash="$(sha256sum "$core" | cut -d ' ' -f 1)"
if [[ "$actual_hash" != "$expected_hash" ]]; then
  echo "Core binary does not match native/core-build.sha256; rebuild the core." >&2
  exit 1
fi

strings_tool="$(command -v strings || command -v llvm-strings || true)"
if [[ -z "$strings_tool" && -n "${ANDROID_NDK_HOME:-}" ]]; then
  for host in linux-x86_64 darwin-x86_64 windows-x86_64; do
    candidate="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/$host/bin/llvm-strings"
    if [[ -x "$candidate" ]]; then strings_tool="$candidate"; break; fi
    if [[ -x "$candidate.exe" ]]; then strings_tool="$candidate.exe"; break; fi
  done
fi
if [[ -z "$strings_tool" ]]; then
  echo "strings/llvm-strings is required to verify the bundled core." >&2
  exit 1
fi
core_strings="$("$strings_tool" "$core")"
if ! grep -F "save-state decoded payload is" <<<"$core_strings" >/dev/null; then
  echo "The core does not contain the current decoded-state diagnostic." >&2
  echo "Rebuild it from native/DingooEmu before packaging the app." >&2
  exit 1
fi
if grep -F "save-state payload exceeds the decoded size limit" <<<"$core_strings" >/dev/null; then
  echo "The core still contains the obsolete 64 MiB decoded-state limit diagnostic." >&2
  exit 1
fi

if ! grep -F "Rejecting save state with an unknown magic or format version" <<<"$core_strings" >/dev/null; then
  echo "The core lacks the official upstream save-state envelope." >&2
  exit 1
fi
if grep -F "dingooemu_frame_rate_enhancement" <<<"$core_strings" >/dev/null; then
  echo "The bundled core still contains the retired fork-only enhancement." >&2
  exit 1
fi

if [[ -n "${ANDROID_NDK_HOME:-}" ]]; then
  readelf=""
  for host in linux-x86_64 darwin-x86_64 windows-x86_64; do
    candidate="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/$host/bin/llvm-readelf"
    if [[ -x "$candidate" ]]; then readelf="$candidate"; break; fi
    if [[ -x "$candidate.exe" ]]; then readelf="$candidate.exe"; break; fi
  done
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

echo "Verified upstream $actual_revision; ARM64 core SHA-256 and 16 KB LOAD alignment."
