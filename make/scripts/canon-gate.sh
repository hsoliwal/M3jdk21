#!/bin/bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
#
# Runs the canon gate in make/jdk/src/classes/build/tools/canongate with a JDK 17 or later.
#
#   bash make/scripts/canon-gate.sh check <packet.tsv>
#   bash make/scripts/canon-gate.sh run <packet.tsv> <run-dir> [<root>]
#   bash make/scripts/canon-gate.sh selftest
#   bash make/scripts/canon-gate.sh <stamp|extension_points|text_refs> <root> <base-ref> <out-dir> [<name>...]
#   bash make/scripts/canon-gate.sh regex_sites <root> <out-dir> [<rev>]
#
# JAVA_HOME selects the JDK; otherwise java and javac come from PATH. <root> defaults to this
# repository. The contract is documented in doc/canon-gate.md.
set -eu

topdir=$(cd "$(dirname "$0")/../.." && pwd)
bin=${JAVA_HOME:+$JAVA_HOME/bin/}
sources=("$topdir"/make/jdk/src/classes/build/tools/canongate/*.java)

case "${1-}" in
  check) [ $# -eq 2 ] || { echo 'usage: canon-gate.sh check <packet.tsv>' >&2; exit 2; }
    args=(check "$2"); main=build.tools.canongate.CanonGate ;;
  run) [ $# -eq 3 ] || [ $# -eq 4 ] || { echo 'usage: canon-gate.sh run <packet.tsv> <run-dir> [<root>]' >&2; exit 2; }
    args=(run "$2" "$3" "${4:-$topdir}"); main=build.tools.canongate.CanonGate ;;
  selftest) [ $# -eq 1 ] || { echo 'usage: canon-gate.sh selftest' >&2; exit 2; }
    sources+=("$topdir"/test/make/canongate/*.java)
    args=(); main=build.tools.canongate.CanonGateTest ;;
  stamp|extension_points|text_refs|regex_sites) args=("$@"); main=build.tools.canongate.CanonGate ;;
  *) echo 'usage: canon-gate.sh check|run|selftest|stamp|extension_points|text_refs|regex_sites ...' >&2; exit 2 ;;
esac

classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
"${bin}javac" -encoding UTF-8 -Xlint:all -Werror -d "$classes" "${sources[@]}"
set +e
"${bin}java" -cp "$classes" "$main" "${args[@]+"${args[@]}"}"
status=$?
exit "$status"
