#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
crate="$(cd "$(dirname "$0")" && pwd)"
maven="${MR_MVN:-mvn}"
options=(-B -ntp)
if [[ -n "${MR_MAVEN_REPO:-}" ]]; then
  options+=(-o "-Dmaven.repo.local=$MR_MAVEN_REPO")
fi
"$maven" "${options[@]}" -f "$crate/pom.xml" test
python3 "$crate/check.py"
python3 "$crate/../../runtime-integration/check-m3string-invariants.py"
