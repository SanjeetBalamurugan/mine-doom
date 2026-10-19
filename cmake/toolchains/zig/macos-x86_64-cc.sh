#!/usr/bin/env bash
exec "$(dirname "${BASH_SOURCE[0]}")/zig-cc.sh" x86_64-macos.11.0 "$@"
