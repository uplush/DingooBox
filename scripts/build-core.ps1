$ErrorActionPreference = "Stop"

$ProjectDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$CoreDir = Join-Path $ProjectDir "native\DingooEmu"
$CoreManifest = Join-Path $CoreDir "Cargo.toml"

if (-not (Test-Path -LiteralPath $CoreManifest)) {
    throw "DingooEmu submodule is not initialized. Run: git submodule update --init --recursive"
}

if (-not $env:ANDROID_NDK_HOME) {
    throw "ANDROID_NDK_HOME must point to Android NDK 28.2.13676358."
}
if (-not (Get-Command cargo -ErrorAction SilentlyContinue)) {
    throw "cargo is required."
}
if (-not (Get-Command cargo-ndk -ErrorAction SilentlyContinue)) {
    throw "cargo-ndk is required."
}

Push-Location $CoreDir
try {
    $PreviousRustFlags = $env:RUSTFLAGS
    $env:RUSTFLAGS = "$PreviousRustFlags -C link-arg=-Wl,-z,max-page-size=16384".Trim()
    cargo ndk `
        -t arm64-v8a `
        --platform 24 `
        -o (Join-Path $ProjectDir "app\src\main\jniLibs") `
        build -p dingooemu-libretro --release
    if ($LASTEXITCODE -ne 0) { throw "DingooEmu core build failed." }
}
finally {
    $env:RUSTFLAGS = $PreviousRustFlags
    Pop-Location
}

Write-Host "Core written to app\src\main\jniLibs\arm64-v8a\libdingooemu.so"
