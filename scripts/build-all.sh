#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLS_DIR="${TOOLS_DIR:-$HOME/.doom-toolchains}"
[ -f "$TOOLS_DIR/env.sh" ] && . "$TOOLS_DIR/env.sh"

BUILD_TYPE="${BUILD_TYPE:-Release}"
JOBS="${JOBS:-$(nproc)}"
ANDROID_API="${ANDROID_API:-24}"
BUILD_ROOT="$ROOT/build-native"

ALL_TARGETS=(linux-x86_64 linux-aarch64 windows-x86_64 macos-x86_64 macos-aarch64 android-arm64-v8a android-armeabi-v7a android-x86_64 android-x86)
TARGETS=("$@")
[ ${#TARGETS[@]} -eq 0 ] && TARGETS=("${ALL_TARGETS[@]}")

: "${JAVA_HOME:?JAVA_HOME not set, run scripts/install-toolchains.sh}"
[ -f "$JAVA_HOME/include/jni.h" ] || { echo "no jni.h in $JAVA_HOME/include, need a full JDK" >&2; exit 1; }

export ZIG ANDROID_NDK_HOME JAVA_HOME
chmod +x "$ROOT"/cmake/toolchains/zig/*.sh "$ROOT"/scripts/*.sh

DOOMJNI_SRC="$ROOT/src/main/java/com/doom/DoomJNI.java"
# if [ -f "$DOOMJNI_SRC" ]; then
#   mkdir -p "$BUILD_ROOT/javac"
#   CP="${JAVAC_CP:-}"
#   if [ -z "$CP" ] && [ -d "$HOME/.gradle" ]; then
#     CP="$(find "$HOME/.gradle" -name 'slf4j-api-*.jar' ! -name '*sources*' 2>/dev/null | head -1 || true)"
#   fi
#   javac -h "$ROOT/native/include" -d "$BUILD_ROOT/javac" -sourcepath "$ROOT/src/main/java" ${CP:+-cp "$CP"} "$DOOMJNI_SRC" \
#     || echo "javac -h failed, using existing header (set JAVAC_CP to an slf4j-api jar)" >&2
# fi

for fn in $(grep -o 'Java_com_doom_DoomJNI_[A-Za-z0-9_]*' "$ROOT/native/include/com_doom_DoomJNI.h" | sort -u); do
  grep -q "$fn" "$ROOT/native/src/doom_jni.c" || { echo "not implemented in native/src/doom_jni.c: $fn" >&2; exit 1; }
done

build_target() {
  local t="$1"
  local bdir="$BUILD_ROOT/$t"
  local args=()

  case "$t" in
    linux-*|windows-*|macos-*)
      args+=(-DCMAKE_TOOLCHAIN_FILE="$ROOT/cmake/toolchains/$t.cmake") ;;
    android-*)
      args+=(-DCMAKE_TOOLCHAIN_FILE="$ROOT/cmake/toolchains/android.cmake" -DANDROID_ABI="${t#android-}" -DANDROID_PLATFORM="android-$ANDROID_API") ;;
    *)
      echo "unknown target: $t" >&2
      return 1 ;;
  esac

  rm -rf "$bdir/out"
  cmake -S "$ROOT/native" -B "$bdir" -G Ninja -DCMAKE_BUILD_TYPE="$BUILD_TYPE" -DJNI_HOME="$JAVA_HOME" "${args[@]}" \
    && cmake --build "$bdir" -j "$JOBS"
}

failed=()
for t in "${TARGETS[@]}"; do
  echo "== $t"
  build_target "$t" || failed+=("$t")
done

"$ROOT/scripts/package.sh"

if [ ${#failed[@]} -gt 0 ]; then
  echo "failed: ${failed[*]}" >&2
  exit 1
fi
