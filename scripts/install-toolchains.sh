#!/usr/bin/env bash
set -euo pipefail

TOOLS_DIR="${TOOLS_DIR:-$HOME/.doom-toolchains}"
NDK_VERSION="${NDK_VERSION:-r27c}"
ZIG_VERSION="${ZIG_VERSION:-0.13.0}"

SUDO=""
[ "$(id -u)" -ne 0 ] && SUDO="sudo"

case "$(uname -m)" in
  x86_64) zig_arch=x86_64 ;;
  aarch64|arm64) zig_arch=aarch64 ;;
  *) echo "unsupported host arch: $(uname -m)" >&2; exit 1 ;;
esac

$SUDO apt-get update
$SUDO apt-get install -y build-essential cmake ninja-build curl unzip xz-utils zip file \
  mingw-w64 gcc-aarch64-linux-gnu openjdk-21-jdk-headless

mkdir -p "$TOOLS_DIR"
cd "$TOOLS_DIR"

if [ ! -d "android-ndk-$NDK_VERSION" ]; then
  curl -fL -o ndk.zip "https://dl.google.com/android/repository/android-ndk-$NDK_VERSION-linux.zip"
  unzip -q ndk.zip
  rm ndk.zip
fi

if [ ! -d "zig-$ZIG_VERSION" ]; then
  curl -fL -o zig.tar.xz "https://ziglang.org/download/$ZIG_VERSION/zig-linux-$zig_arch-$ZIG_VERSION.tar.xz"
  mkdir "zig-$ZIG_VERSION"
  tar -xf zig.tar.xz -C "zig-$ZIG_VERSION" --strip-components=1
  rm zig.tar.xz
fi

JAVA_HOME_DIR="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"

cat > "$TOOLS_DIR/env.sh" <<ENV
export ANDROID_NDK_HOME="$TOOLS_DIR/android-ndk-$NDK_VERSION"
export ZIG="$TOOLS_DIR/zig-$ZIG_VERSION/zig"
export JAVA_HOME="$JAVA_HOME_DIR"
ENV

echo "done: $TOOLS_DIR/env.sh"
