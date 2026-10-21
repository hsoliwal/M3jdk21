<!-- SPDX-License-Identifier: Apache-2.0 -->
# Receiving recipe: owned Commons HashBag

`com.synexia.m3.OwnedApacheHashBag20261019` runs the existing first-party
`M3HashPinnedJavaSnapshotRecipe` engine (compiled in place from
`../owned-guava-evicting-queue-20261018/engine-src`, Git blob `d58ca50d`, not copied) in PlainText
custody mode with crate `owned-apache-hash-bag`.

Manifest: the generated scaffold preimage (SHA-256 `c55a78c2...`, 5711 bytes) is replaced by the owned
postimage (SHA-256 `b35a43e1...`, 18277 bytes); an existing owned postimage is accepted unchanged.
A missing target, drift on either side and duplicate targets are refused before any output.

`mvn -f pom.xml test`: 2 tests. The first covers 8 executions (preimage or postimage, both source orders,
with and without an unrelated file), each followed by a fresh re-run that changes nothing (fixed point).
The second covers the 4 refusals. API behavior is qualified by the module's own tests.
