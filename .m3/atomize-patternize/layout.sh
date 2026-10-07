#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Shared source-layout helpers for the M3 atom/pattern receiver.

m3_java_root() {
  local rel="${1:?java path required}"
  local rest module platform

  case "$rel" in
    src/main/java/*)
      printf '%s\n' 'src/main/java'
      return
      ;;
    */src/main/java/*)
      printf '%s/src/main/java\n' "${rel%%/src/main/java/*}"
      return
      ;;
    src/test/java/*)
      printf '%s\n' 'src/test/java'
      return
      ;;
    */src/test/java/*)
      printf '%s/src/test/java\n' "${rel%%/src/test/java/*}"
      return
      ;;
    src/it/java/*)
      printf '%s\n' 'src/it/java'
      return
      ;;
    */src/it/java/*)
      printf '%s/src/it/java\n' "${rel%%/src/it/java/*}"
      return
      ;;
    src/integrationTest/java/*)
      printf '%s\n' 'src/integrationTest/java'
      return
      ;;
    */src/integrationTest/java/*)
      printf '%s/src/integrationTest/java\n' "${rel%%/src/integrationTest/java/*}"
      return
      ;;
  esac

  # OpenJDK product roots: src/<module>/<share|unix|windows|...>/classes/**.
  if [[ "$rel" == src/*/*/classes/* ]]; then
    rest="${rel#src/}"
    module="${rest%%/*}"
    rest="${rest#*/}"
    platform="${rest%%/*}"
    if [[ -n "$module" && -n "$platform" && "$rest" == "$platform/classes/"* ]]; then
      printf 'src/%s/%s/classes\n' "$module" "$platform"
      return
    fi
  fi

  # OpenJDK build-tool Java roots.
  if [[ "$rel" == make/*/src/classes/* ]]; then
    rest="${rel#make/}"
    module="${rest%%/*}"
    printf 'make/%s/src/classes\n' "$module"
    return
  fi

  # OpenJDK test families. Keep the stable harness root, not each package directory.
  case "$rel" in
    test/hotspot/jtreg/*) printf '%s\n' 'test/hotspot/jtreg'; return ;;
    test/hotspot/gtest/*) printf '%s\n' 'test/hotspot/gtest'; return ;;
    test/jdk/*) printf '%s\n' 'test/jdk'; return ;;
    test/langtools/*) printf '%s\n' 'test/langtools'; return ;;
    test/jaxp/*) printf '%s\n' 'test/jaxp'; return ;;
    test/micro/*) printf '%s\n' 'test/micro'; return ;;
    test/lib/*) printf '%s\n' 'test/lib'; return ;;
  esac

  dirname -- "$rel"
}

m3_root_has_pom() {
  local repo="${1:?repository required}"
  local root="${2:?root required}"
  local current="$repo/$root"

  while [[ "$current" == "$repo"/* || "$current" == "$repo" ]]; do
    [[ -f "$current/pom.xml" && ! -L "$current/pom.xml" ]] && return 0
    [[ "$current" == "$repo" ]] && break
    current="$(dirname -- "$current")"
  done
  return 1
}

m3_collect_external_roots() {
  local repo="${1:?repository required}"
  local roots_file="${2:?roots file required}"
  local output="${3:?output file required}"
  local root

  : > "$output"
  while IFS= read -r root; do
    [[ -n "$root" ]] || continue
    if ! m3_root_has_pom "$repo" "$root"; then
      printf '%s\n' "$root" >> "$output"
    fi
  done < "$roots_file"
  LC_ALL=C sort -u "$output" -o "$output"
}
