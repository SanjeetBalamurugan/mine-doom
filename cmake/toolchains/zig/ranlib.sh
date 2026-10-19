#!/usr/bin/env bash
exec "${ZIG:-zig}" ranlib "$@"
