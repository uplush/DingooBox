#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd "$script_dir/.." && pwd)"

if [[ -z "${ANDROID_NDK_HOME:-}" ]]; then
  echo "ANDROID_NDK_HOME must point to Android NDK 28.2.13676358." >&2
  exit 1
fi

command -v cargo >/dev/null || { echo "cargo is required." >&2; exit 1; }
command -v cargo-ndk >/dev/null || { echo "cargo-ndk is required." >&2; exit 1; }

cd "$project_dir/native/DingooEmu"
RUSTFLAGS="${RUSTFLAGS:-} -C link-arg=-Wl,-z,max-page-size=16384" \
  cargo ndk \
    -t arm64-v8a \
    --platform 24 \
    -o "$project_dir/app/src/main/jniLibs" \
    build -p dingooemu-libretro --release

echo "Core written to app/src/main/jniLibs/arm64-v8a/libdingooemu.so"
