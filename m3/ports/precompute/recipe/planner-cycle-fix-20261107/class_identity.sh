#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Untouched-classes gate: every class file of a baseline build of the module (an untouched copy of master, built with
# the same JDK and pom: <baseline-module-dir>/target/{classes,test-classes}) must be byte-identical (sha256) in this
# module's build; class files present only in this build are listed as NEW (the relocated family). Prints
# CLASS_IDENTITY PASS|FAIL with the counts. Copied from recipe planner-family-20261107 with one addition: class files
# whose relative path matches the declared pattern (the classes a hash-pinned edit of this recipe changes on purpose)
# are listed as CHANGED and do not fail the gate; any other difference does.
# usage: class_identity.sh <baseline-module-dir> <declared-changed-regex>   (after mvn package in both trees)
set -euo pipefail
MODULE=$(cd "$(dirname "$0")/../.." && pwd)
BASE=$(cd "$1" && pwd)
DECLARED=$2
changed=0
same=0
differ=0
new=0
for scope in classes test-classes; do
    [ -d "$BASE/target/$scope" ] || continue
    while read -r rel; do
        if [ ! -f "$MODULE/target/$scope/$rel" ]; then
            differ=$((differ + 1)); echo "MISSING $scope/$rel"
        elif cmp -s "$BASE/target/$scope/$rel" "$MODULE/target/$scope/$rel"; then
            same=$((same + 1))
        elif [[ "$scope/$rel" =~ $DECLARED ]]; then
            changed=$((changed + 1)); echo "CHANGED $scope/$rel (declared)"
        else
            differ=$((differ + 1)); echo "DIFFERS $scope/$rel"
        fi
    done < <(cd "$BASE/target/$scope" && find . -name '*.class' | sed 's#^\./##' | sort)
    while read -r rel; do
        [ -f "$BASE/target/$scope/$rel" ] || { new=$((new + 1)); echo "NEW $scope/$rel"; }
    done < <(cd "$MODULE/target/$scope" && find . -name '*.class' | sed 's#^\./##' | sort)
done
if [ "$differ" -eq 0 ]; then
    echo "CLASS_IDENTITY PASS identical=$same changed=$changed new=$new"
else
    echo "CLASS_IDENTITY FAIL identical=$same changed=$changed differ=$differ new=$new"
    exit 1
fi
