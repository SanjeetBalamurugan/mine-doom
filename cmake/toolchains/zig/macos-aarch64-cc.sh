#!/usr/bin/env bash
exec "$(dirname "${BASH_SOURCE[0]}")/zig-cc.sh" aarch64-macos.11.0 "$@"
