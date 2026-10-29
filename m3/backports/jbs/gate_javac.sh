#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
# Atom JAVAC_PATCH_MODULE_GATE (baseline-aware, one javac per module): export the Java class sources a batch changes at
# the base and at the tip (git archive), compile each module's files with
# `javac --release 21 --patch-module <module>=<share;windows roots>` against the local JDK 21 image, and compare
# -XDrawDiagnostics error keys (file, code, args; no line numbers). PASS = no tip error the base lacks.
# Every compile also patches the batch's other modules with their exported sources (cross-module API changes in one
# batch resolve; -implicit:none keeps their classes out of the output). Modules that ct.sym for --release 21 omits (internal or Windows-only: jdk.internal.opt users, jdk.attach,
# jdk.crypto.mscapi, jdk.internal.vm.ci) are compiled by the same JDK 21 javac on the full image (mode jrt21, class
# file 65). A javac error without a file location (module not found, crash) is a sentinel: it fails the gate and is
# never cancelled by the base. Only src/<module>/{share,windows}/classes/**.java compile on this box; others NOT_RUN.
# Usage: gate_javac.sh <repo> <base rev> <tip rev> <out dir>
set -uo pipefail
R="$1"; B="$2"; TIP="$3"; O="$4"; JDK="${JDK21:-/c/Program Files/Java/jdk-21}"; export GIT_NO_LAZY_FETCH=1
rm -rf "${O:?}"; mkdir -p "$O"
git -C "$R" diff --name-only --diff-filter=AMR "$B" "$TIP" | grep -E '^src/[^/]+/(share|windows)/classes/.*\.java$' > "$O/files.txt"
echo "files $(wc -l < "$O/files.txt") modules $(cut -d/ -f2 "$O/files.txt" | sort -u | wc -l)"
compile() { # side module roots mode
	local s="$1" m="$2" roots="$3" mode="$4" rel=()
	[ "$mode" = release21 ] && rel=(--release 21)
	"$JDK/bin/javac" -J-Xms16m -J-Xmx768m -J-XX:+UseSerialGC -J-XX:TieredStopAtLevel=1 "${rel[@]}" \
		--patch-module "$m=$roots" "${PEERS[@]}" -implicit:none -XDrawDiagnostics -Xmaxerrs 100000 -nowarn \
		-d "$(cygpath -m "$O/$s/classes/$m")" \
		$(grep "^src/$m/" "$O/$s.present" | sed "s#^#$(cygpath -m "$O/$s")/#") > "$O/$s.$m.diag" 2>&1
}
run() { # side rev
	local s="$1" rev="$2" m os roots mode rc
	mkdir -p "$O/$s"; git -C "$R" ls-tree -r --name-only "$rev" -- $(cat "$O/files.txt") > "$O/$s.present"
	[ -s "$O/$s.present" ] && git -C "$R" archive --format=tar "$rev" -- $(cat "$O/$s.present") | tar -x -C "$O/$s"
	: > "$O/$s.diag"
	for m in $(cut -d/ -f2 "$O/$s.present" | sort -u); do
		roots=""
		for os in share windows; do
			[ -d "$O/$s/src/$m/$os/classes" ] && roots="$roots${roots:+;}$(cygpath -m "$O/$s/src/$m/$os/classes")"
		done
		PEERS=()   # the batch's other modules as source patches, so cross-module API changes in one batch resolve
		for o in $(cut -d/ -f2 "$O/$s.present" | sort -u); do
			[ "$o" = "$m" ] || [ ! -d "$O/$s/src/$o/share/classes" ] || PEERS+=(--patch-module "$o=$(cygpath -m "$O/$s/src/$o/share/classes")")
		done
		mode=release21; compile "$s" "$m" "$roots" "$mode"; rc=$?
		if grep -q 'compiler.err.module.not.found' "$O/$s.$m.diag"; then mode=jrt21; compile "$s" "$m" "$roots" "$mode"; rc=$?; fi
		echo "$s $m mode=$mode rc=$rc errors=$(grep -c 'compiler\.err' "$O/$s.$m.diag")"
		sed "s#^- #$m: - #" "$O/$s.$m.diag" >> "$O/$s.diag"
	done
}
run base "$B"; run tip "$TIP"
key() { grep -E ':[0-9]+:[0-9]+: compiler\.err' "$1" | sed -E 's#^.*/(base|tip)/src/##; s#:[0-9]+:[0-9]+: #\t#' | sort -u; }
key "$O/base.diag" > "$O/base.keys"; key "$O/tip.diag" > "$O/tip.keys"
comm -13 "$O/base.keys" "$O/tip.keys" > "$O/new.keys"
grep -E '^[A-Za-z0-9.]+: - compiler\.err' "$O/tip.diag" | sort -u > "$O/sentinel.keys"
echo "base_errors $(wc -l < "$O/base.keys") tip_errors $(wc -l < "$O/tip.keys") new_errors $(wc -l < "$O/new.keys") sentinels $(wc -l < "$O/sentinel.keys")"
if [ -s "$O/new.keys" ] || [ -s "$O/sentinel.keys" ]; then echo "GATE FAIL"; cat "$O/new.keys" "$O/sentinel.keys" | head -20; exit 1; fi
echo "GATE PASS"
