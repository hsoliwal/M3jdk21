<!-- SPDX-License-Identifier: Apache-2.0 -->
# Receiving recipe: widened Spring single-value view (proposal)

`com.synexia.m3.SpringSingleValueViewWiden20261019` runs the existing first-party
`M3HashPinnedJavaSnapshotRecipe` engine (compiled in place from
`../owned-guava-evicting-queue-20261018/engine-src`, Git blob `d58ca50d`, not copied) in PlainText
custody mode with crate `spring-single-value-view-widen`.

Manifest: the master `M3SingleValueMapView` preimage (SHA-256 `6f33bb30...`, 10369 bytes, the postimage of
`owned-spring-multivalue-map-20261017`) is replaced by the widened postimage (SHA-256 `f79e3637...`,
10873 bytes); an existing widened postimage is accepted unchanged. A missing target, drift on either side
and duplicate targets are refused before any output.

`mvn -f pom.xml test`: 2 tests. The first covers 8 executions (preimage or postimage, both source orders,
with and without an unrelated file), each followed by a fresh re-run that changes nothing (fixed point).
The second covers the 4 refusals. View behavior is qualified by
`qualification/spring-single-value-view-widen-proposal-20261019`.
