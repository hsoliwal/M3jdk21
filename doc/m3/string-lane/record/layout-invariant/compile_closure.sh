#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
#
# COMPILE_CLOSURE v1 (2026-11-17): javac-checks java.lang.String of M3jdk21 master (or an edited copy) against the
# stock JDK 21 java.base, pulling every master java.base source javac names as missing, until it compiles or no new
# file can be found. usage: compile_closure.sh <work> <seed-list> [<override-dir>]
#   <work>/src            master sources fetched so far (git show origin/master:<path>, never a checkout)
#   <seed-list>           java/lang file names to fetch first (one per line)
#   <override-dir>        files that replace the fetched copy (the edited String.java etc.), same relative paths
# Output: <work>/javac.log (last run), <work>/FETCHED.txt, exit 0 when javac exits 0.
set -u
work=$1; seed=$2; over=${3:-}
repo=/c/m3-superset/conv/m3jdk21
jdk="C:/Program Files/Java/jdk-21"
base=src/java.base/share/classes
mkdir -p "$work/src" "$work/out"
fetch() { # <relative path under java.base classes>
  local rel=$1 dst="$work/src/$1"
  [ -f "$dst" ] && return 1
  mkdir -p "$(dirname "$dst")"
  (cd "$repo" && git show "origin/master:$base/$rel" > "$dst" 2>/dev/null) || { rm -f "$dst"; return 1; }
  echo "$rel" >> "$work/FETCHED.txt"; return 0
}
while read -r f; do [ -n "$f" ] && fetch "java/lang/$f"; done < "$seed"
for round in $(seq 1 25); do
  [ -n "$over" ] && (cd "$over" && find . -name '*.java' | while read -r o; do mkdir -p "$work/src/$(dirname "$o")"; cp "$o" "$work/src/$o"; done)
  rm -rf "$work/out"; mkdir -p "$work/out"
  files=$(cd "$work/src" && find . -name '*.java' | sed 's#^\./##')
  (cd "$work/src" && MSYS_NO_PATHCONV=1 "$jdk/bin/javac" --patch-module "java.base=$(cygpath -m "$work/src")" \
     -XDignore.symbol.file -Xlint:all,-this-escape -Werror -Xmaxerrs 2000 -nowarn -implicit:none \
     -d "$(cygpath -m "$work/out")" $files) > "$work/javac.log" 2>&1
  rc=$?
  echo "round $round javac_exit=$rc errors=$(grep -c ': error:' "$work/javac.log")"
  [ $rc -eq 0 ] && exit 0
  # missing types: "symbol:   class X" with "location: package a.b" or imports "package a.b does not exist"
  new=0
  [ -s "$work/INDEX.txt" ] || (cd "$repo" && git ls-tree -r --name-only origin/master -- "$base" \
    | sed "s#^$base/##") > "$work/INDEX.txt"
  for cand in $(python -I "$(dirname "$0")/resolve_missing.py" "$work/javac.log" "$work/INDEX.txt"); do
    case "$cand" in
      PKG:*) p=${cand#PKG:}
             for f in $(cd "$repo" && git ls-tree --name-only "origin/master:$base/$p" 2>/dev/null | grep '\.java$'); do
               fetch "$p/$f" && new=$((new+1)); done ;;
      *) fetch "$cand" && new=$((new+1)) ;;
    esac
  done
  echo "  fetched $new new files"
  [ $new -eq 0 ] && exit 1
done
exit 1
