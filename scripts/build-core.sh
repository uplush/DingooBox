#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd "$script_dir/.." && pwd)"
core_dir="$project_dir/native/DingooEmu"

if [[ ! -f "$core_dir/Cargo.toml" ]]; then
  echo "DingooEmu submodule is not initialized. Run: git submodule update --init --recursive" >&2
  exit 1
fi

if [[ -z "${ANDROID_NDK_HOME:-}" ]]; then
  echo "ANDROID_NDK_HOME must point to Android NDK 28.2.13676358." >&2
  exit 1
fi

command -v cargo >/dev/null || { echo "cargo is required." >&2; exit 1; }
command -v cargo-ndk >/dev/null || { echo "cargo-ndk is required." >&2; exit 1; }

expected_revision="$(tr -d '\r\n' < "$project_dir/native/UPSTREAM_COMMIT")"
if [[ "$(git -C "$core_dir" rev-parse HEAD)" != "$expected_revision" ]]; then
  echo "Core checkout must match native/UPSTREAM_COMMIT before building." >&2
  exit 1
fi
if [[ -n "$(git -C "$core_dir" status --porcelain --untracked-files=no)" ]]; then
  echo "Official core source must be unmodified before building." >&2
  exit 1
fi

cd "$core_dir"
RUSTFLAGS="${RUSTFLAGS:-} -C link-arg=-Wl,-z,max-page-size=16384" \
  cargo ndk \
    -t arm64-v8a \
    --platform 24 \
    -o "$project_dir/app/src/main/jniLibs" \
    build -p dingooemu-libretro --release --locked

sha256sum "$project_dir/app/src/main/jniLibs/arm64-v8a/libdingooemu.so" \
  | cut -d ' ' -f 1 > "$project_dir/native/core-build.sha256"

echo "Core written to app/src/main/jniLibs/arm64-v8a/libdingooemu.so"
