#!/bin/bash
# Gate for receiving m3/tooling/search: the staged module's own verify scripts plus the three stacked
# com.synexia crate proofs (empty-keys, empty-range, lower-reuse) run on a mirror of the crate layout.
set -u
cd "$(dirname "$0")"
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 LANG=C.UTF-8 LC_ALL=C.UTF-8
export PATH=$JAVA_HOME/bin:$PATH
run() { local name=$1; shift; "$@" > "logs/$name.log" 2>&1; local rc=$?; printf 'GATE\t%s\trc=%s\t%s\n' "$name" "$rc" "$(tail -1 "logs/$name.log" | cut -c1-160)"; }
mkdir -p logs
run module-int-verify bash stage/m3/tooling/search/verify.sh
run module-long-verify bash stage/m3/tooling/search/verify-long-bounds.sh
R=synroot/synexia-openrewrite-recipes/recipe-crates
run crate-empty-keys bash $R/m3jdk-sorted-empty-keys-20261101/verification/verify-jni.sh
run crate-empty-range bash $R/m3jdk-sorted-empty-range-20261101/verification/verify-jni.sh
run crate-lower-reuse bash $R/m3jdk-sorted-lower-reuse-20261102/verification/verify-jni.sh
