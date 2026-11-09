#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Run a checked-out m3/collections test contract against the owner sources of another commit (DECISION.tsv evidence).
# usage: probe_owner.sh <commit> <ContractSimpleName> [mode...]
# env: REPO (git repository, default .), WORK (scratch dir, required), JAVA_BIN (JDK 21 bin dir, default: PATH),
#      JVM_OPT (one extra JVM option for javac -J and java, e.g. -Djava.io.tmpdir=<dir>),
#      CONTRACTS (dir holding the contract .java, default $REPO/m3/collections/src/test/java/jdk/m3/collections)
# The owner tree of <commit> (m3/collections + m3/arrays main sources, module-info dropped) is the -sourcepath;
# javac -implicit:class compiles only the contract's closure. Trees before the JDK naming rename (m3/tooling/jdk-naming)
# hold the owner under its old package: the contract's package line follows the <x>/m3/collections directory of the tree. Prints "<commit> <mode> :: <first two output lines>"; COMPILE_FAIL when it does not build.
set -u
REPO=${REPO:-.}; WORK=${WORK:?set WORK to a scratch directory}; JB=${JAVA_BIN:+$JAVA_BIN/}
JO=(); JJ=(); [ -n "${JVM_OPT:-}" ] && JO=("$JVM_OPT") && JJ=("-J$JVM_OPT")
CONTRACTS=${CONTRACTS:-$REPO/m3/collections/src/test/java/jdk/m3/collections}
c=$(git -C "$REPO" rev-parse --short=11 "$1") || exit 2; contract=$2; shift 2
d=$WORK/$c
if [ ! -d "$d/src" ]; then
  mkdir -p "$d/src"
  git -C "$REPO" archive "$c" -- m3/collections/src/main/java m3/arrays/src/main/java | tar -x -C "$d/src" || exit 2
  find "$d/src" -name module-info.java -delete
fi
pdir=$(cd "$d/src/m3/collections/src/main/java" && find . -mindepth 3 -maxdepth 3 -type d -path './*/m3/collections' \
  | sort | tail -1 | cut -c3-)
[ -n "$pdir" ] || { echo "$c NO_OWNER_PACKAGE"; exit 3; }
pkg=$(printf '%s' "$pdir" | tr / .)
rm -rf "$d/t-$contract" "$d/out-$contract"; mkdir -p "$d/t-$contract/$pdir" "$d/out-$contract"
sed "s/^package jdk\.m3\.collections;/package $pkg;/" "$CONTRACTS/$contract.java" > "$d/t-$contract/$pdir/$contract.java"
sep=:; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) sep=";";; esac
sp="$d/src/m3/collections/src/main/java$sep$d/src/m3/arrays/src/main/java"
if ! "${JB}javac" "${JJ[@]}" --release 21 -nowarn -implicit:class -proc:none -d "$d/out-$contract" -sourcepath "$sp" \
    "$d/t-$contract/$pdir/$contract.java" > "$d/javac-$contract.log" 2>&1; then
  echo "$c COMPILE_FAIL $(grep -m1 error "$d/javac-$contract.log" | cut -c1-200)"; exit 3
fi
for m in "$@"; do
  echo "$c $m :: $("${JB}java" "${JO[@]}" -ea -cp "$d/out-$contract" "$pkg.$contract" "$m" 2>&1 | head -2 | tr '\n\t' '  ' | cut -c1-230)"
done
