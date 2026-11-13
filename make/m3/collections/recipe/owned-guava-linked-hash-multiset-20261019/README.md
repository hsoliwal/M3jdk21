<!-- SPDX-License-Identifier: Apache-2.0 -->
# Receiving recipe: owned Guava LinkedHashMultiset

`com.synexia.m3.OwnedGuavaLinkedHashMultiset20261019` runs the existing first-party
`M3HashPinnedJavaSnapshotRecipe` engine (compiled in place from
`../owned-guava-evicting-queue-20261018/engine-src`, Git blob `d58ca50d`, not copied) in PlainText
custody mode with crate `owned-guava-linked-hash-multiset`.

Manifest: the generated scaffold preimage (SHA-256 `8f877a27...`, 6452 bytes) is replaced by the owned
postimage (SHA-256 `422fe89e...`, 25403 bytes); an existing owned postimage is accepted unchanged.
A missing target, drift on either side and duplicate targets are refused before any output.

`mvn -f pom.xml test`: 2 tests. The first covers 8 executions (preimage or postimage, both source orders,
with and without an unrelated file), each followed by a fresh re-run that changes nothing (fixed point).
The second covers the 4 refusals. API behavior is qualified by the module's own tests.
