#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# In-JDK compactor receiver: exact current-master java.util call sites + focused Java 21 proof.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../../../.."

verify_blob() {
  local expected=$1 path=$2 actual
  actual=$(git hash-object "$path")
  if [[ "$actual" != "$expected" ]]; then
    echo "REFUSAL current java.util source drift $path: $actual != $expected" >&2
    exit 1
  fi
}

# The caller owns predicate execution, modCount and tail clearing.
# A change to either current java.util owner requires its own reviewed pass.
verify_blob 885d891b0f817080824622d592691a27cf8ffa26 src/java.base/share/classes/java/util/ArrayList.java
verify_blob a79a77ce8700dc494d02d906b2a7765670629eee src/java.base/share/classes/java/util/Vector.java
verify_blob 46eef59604fc52dae7dea11de615dffc5e90d943 test/jdk/java/util/ArrayList/M3ArrayListVectorGcMutationContract.java
verify_blob 5bb6fb3ee9e7a3e3ce73bdbf2439eb209e126302 test/jdk/java/util/ArrayList/M3CollectionMaskGcBarrierContract.java
for owner in ArrayList Vector; do
  source="src/java.base/share/classes/java/util/$owner.java"
  grep -Fq 'import jdk.internal.mindex.M3CollectionMaskCompactor;' "$source"
  grep -Fq 'w = M3CollectionMaskCompactor.compact(es, beg, end, deathRow);' "$source"
done

# The historical proof script is reused, never rewritten or weakened.
bash m3/collections/qualification/java-base-mask-20261023/verify.sh
# The public GC oracle is a stock-JDK reference here; a separate jtreg
# workflow must run it against the actual source-built M3JDK21 image.
build=$(mktemp -d)
trap 'rm -rf -- "$build"' EXIT
javac --release 21 -Xlint:all -Werror -d "$build" \
  test/jdk/java/util/ArrayList/M3CollectionMaskGcBarrierContract.java
java -Xms24m -Xmx96m -XX:+UseG1GC -cp "$build" M3CollectionMaskGcBarrierContract
java -Xms24m -Xmx96m -XX:+UseSerialGC -cp "$build" M3CollectionMaskGcBarrierContract
echo "PASS current-master java.util callsite custody and selected java.base mask contracts"
echo "NOTE matched M3JDK21 product-image jtreg, GC/barrier, and benchmarks remain unqualified"
