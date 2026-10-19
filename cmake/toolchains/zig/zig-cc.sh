#!/usr/bin/env bash
target="$1"
shift

args=()
skip=0
for a in "$@"; do
  if [ "$skip" = 1 ]; then
    skip=0
    continue
  fi
  case "$a" in
    -install_name) skip=1 ;;
    -Wl,-install_name,*|-Wl,-headerpad_max_install_names|-Wl,-search_paths_first) ;;
    *) args+=("$a") ;;
  esac
done

exec "${ZIG:-zig}" cc -target "$target" "${args[@]}"
