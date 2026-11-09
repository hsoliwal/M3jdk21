#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
# Atom (pattern JDK_MODULE_PATCH_GATE): baseline-aware javac gate of chosen files of one JDK module at two commits
# (candidate, baseline) of a partial clone. Each commit gets a sparse no-checkout worktree holding only
# src/<module>/share/classes (missing blobs prefetched in one batch, never one lazy fetch per blob), then
# javac --patch-module <module>=<that tree> compiles the files against a stock JDK 21 (classes the module generates
# at build time come from the running JDK). Error keys are "<file>|<message>" without line numbers; the verdict is
# PASS when the candidate has no key beyond the baseline. Takes the shared mkdir lock K:/m3-work/tmp/jvm.lock when it
# frees within 120 s, else runs anyway (a few-file compile), printing a heartbeat every 30 s while waiting.
# DEPS="<module> ..." (environment, optional): modules whose share/classes of the SAME commit are patched in as well, for
# sources that use an API the range adds to another module (2026-10-30: jdk.crypto.ec needs java.base SHA3.SHAKE256,
# jdk.javadoc needs jdk.compiler InheritDocTree.getSupertype; without DEPS they fail against the stock JDK 21 modules).
# Usage: jdk_module_gate.sh <clone> <candidate> <baseline> <module> <out dir> <file>...
set -uo pipefail
C="$1"; CAND="$2"; BASE="$3"; MOD="$4"; OUT="$5"; shift 5; JAVAC="C:/Program Files/Java/jdk-21/bin/javac"; LOCK=K:/m3-work/tmp/jvm.lock
mkdir -p "$OUT"; export MSYS_NO_PATHCONV=1
tree_for() { # tree_for <commit> <dir>
	local h="$1" d="$2"
	[ -e "$d/.git" ] || git -C "$C" worktree add --no-checkout --detach "$d" "$h" > /dev/null 2>&1 || return 1
	git -C "$d" config core.autocrlf false; git -C "$d" sparse-checkout init --no-cone > /dev/null 2>&1
	git -C "$d" sparse-checkout set $(for m in $MOD ${DEPS:-}; do printf 'src/%s/share/classes/ ' "$m"; done) > /dev/null 2>&1
	git -C "$C" ls-tree -r "$h" -- $(for m in $MOD ${DEPS:-}; do printf 'src/%s/share/classes/ ' "$m"; done) | awk '{ print $3 }' |
		GIT_NO_LAZY_FETCH=1 git -C "$C" cat-file --batch-check 2>/dev/null | awk '$2 == "missing" { print $1 }' > "$d.missing"
	[ -s "$d.missing" ] && git -C "$C" -c fetch.negotiationAlgorithm=noop fetch -q origin --no-tags --no-write-fetch-head --recurse-submodules=no --filter=blob:none --stdin < "$d.missing"
	git -C "$d" checkout -q --detach "$h" 2>&1 | tail -2; }
keys() { # keys <log> <tree dir>: error keys without line numbers
	grep -E '^[^ ].*\.java:[0-9]+: error: ' "$1" | sed -E "s|^.*/src/$MOD/share/classes/||; s|:[0-9]+: error: |\||" | sort; }
held=0; for i in $(seq 1 4); do mkdir "$LOCK" 2> /dev/null && { held=1; break; }; echo "waiting for $LOCK ($((i * 30)) s)"; sleep 30; done
[ $held = 1 ] && trap 'rmdir "$LOCK" 2> /dev/null' EXIT
for side in cand base; do
	h=$CAND; [ $side = base ] && h=$BASE; d="$OUT/wt-$side"
	tree_for "$h" "$d" || { echo "GATE_FAIL worktree $side"; exit 2; }
	rm -rf "$d/.jb"; mkdir -p "$d/.jb"
	timeout 1800 "$JAVAC" -J-Xmx3g -encoding UTF-8 -proc:none -nowarn -implicit:none -Xmaxerrs 100000 -XDshould-stop.ifError=FLOW \
		$(for m in $MOD ${DEPS:-}; do printf -- '--patch-module %s=%s ' "$m" "$d/src/$m/share/classes"; done) -d "$d/.jb" $(for f in "$@"; do printf '%s ' "$d/$f"; done) > "$OUT/javac-$side.log" 2>&1
	echo "$side javac exit $? errors $(grep -c ': error: ' "$OUT/javac-$side.log")"
	keys "$OUT/javac-$side.log" > "$OUT/keys-$side.txt"
done
comm -23 "$OUT/keys-cand.txt" "$OUT/keys-base.txt" > "$OUT/keys-new.txt"
n=$(wc -l < "$OUT/keys-new.txt"); [ "$n" = 0 ] && echo "PASS (no error beyond the baseline)" || { echo "FAIL $n new error keys:"; head -40 "$OUT/keys-new.txt"; }
