#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_ROOT="$ROOT/build-native"
DIST="$ROOT/dist"

rm -rf "$DIST"
mkdir -p "$DIST/native"

for bdir in "$BUILD_ROOT"/*-*/; do
  t="$(basename "$bdir")"
  case "$t" in
    linux-x86_64)        dir=linux-x64 ;;
    linux-aarch64)       dir=linux-arm64 ;;
    windows-x86_64)      dir=windows-x64 ;;
    macos-x86_64)        dir=macos-x64 ;;
    macos-aarch64)       dir=macos-arm64 ;;
    android-arm64-v8a)   dir=android-arm64 ;;
    android-armeabi-v7a) dir=android-arm32 ;;
    android-x86_64)      dir=android-x64 ;;
    android-x86)         dir=android-x86 ;;
    *) continue ;;
  esac
  case "$t" in
    windows-*) lib=doomjni.dll ;;
    macos-*)   lib=libdoomjni.dylib ;;
    *)         lib=libdoomjni.so ;;
  esac
  [ -f "$bdir/out/$lib" ] || continue
  mkdir -p "$DIST/native/$dir"
  cp "$bdir/out/$lib" "$DIST/native/$dir/"
done

(cd "$DIST" && find native -type f | sort | xargs -r sha256sum > SHA256SUMS)
(cd "$DIST" && zip -qr doomjni-natives.zip native SHA256SUMS)

if [ "${INSTALL_RESOURCES:-0}" = 1 ]; then
  RES="$ROOT/src/main/resources"
  mkdir -p "$RES"
  rm -rf "$RES/native"
  cp -r "$DIST/native" "$RES/native"
fi

find "$DIST/native" -type f -exec file {} \;
echo "$DIST/doomjni-natives.zip"