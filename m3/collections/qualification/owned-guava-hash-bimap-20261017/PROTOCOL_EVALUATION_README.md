# Guava HashBiMap protocol evaluation

The Android-derived owned body matches the actual Guava 33.4.8 Android binary in the selected 66-case corpus: 66 equivalent, zero differences, zero invariant or common API failures. Against the actual 33.4.8 JRE binary, the same cases produce 28 equivalent and 38 different outcomes, zero invariant failures and one oracle-side common API failure. These are root-executed results reported to this source-only reviewer, not a full API, JVM, GC, performance or JRE-equivalence admission.

[PROTOCOL_EVALUATION.tsv](PROTOCOL_EVALUATION.tsv) records all 38 differing case IDs mechanically supplied from the executed JRE log. [PROTOCOL_EVALUATION_RECEIPT.json](PROTOCOL_EVALUATION_RECEIPT.json) pins that input log SHA-256 and the source blobs. Full paired returns, error classes, callback traces and forward/inverse identity/order states remain in the root-created `protocol-runtime-inventory.json`. This ledger does not invent or reproduce unprovided paired subvalues.

| Category | Actual differing cases | Source mechanism to review |
|---|---:|---|
| Bulk replaceAll protocol | 12 | JRE clear/rebuild versus Android guarded in-place entry updates |
| Callback iteration protocol | 10 | Direct JRE node traversal versus Android default Map entry iteration |
| Representative reference identity | 8 | Supplied representatives in replacement nodes versus existing array entries |
| Iterator after external replacement | 6 | Revision/node lifetime versus indexed in-place update |
| Oracle contract bug | 1 | Inverse entry setter leaves the iterator removal node stale |
| Inverse forcePut order-labelled case | 1 | Equal-but-distinct inverse key and occupied forward key |

These categories distinguish observed version behavior from a demonstrated API-contract failure. Callback mutation, external-iterator mutation, representative identity and bulk-operation exceptions need explicit scope decisions; this ledger does not classify every difference as a bug or silently declare it acceptable.

The oracle contract bug is `inverse/view-lifecycle/2`. The sequence obtains an inverse entry, changes its value to a new unique forward key, then removes that returned entry through its iterator. [JDK 21 Map.entrySet](https://github.com/openjdk/jdk21u/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/Map.java#L374) expressly permits an iterator-returned entry's setValue during iteration and supports removal through that iterator. No external Map.put, duplicate payload or illegal repeated initial removal is needed to reach the first failing remove.

In the pinned [JRE inverse setter](https://github.com/google/guava/blob/f06690fa3e874f65515e8fd338a74d636e2c792f/guava/src/com/google/common/collect/HashBiMap.java#L718), replacing the delegate updates expectedModCount but does not update the enclosing iterator's toRemove field. [Itr.remove](https://github.com/google/guava/blob/f06690fa3e874f65515e8fd338a74d636e2c792f/guava/src/com/google/common/collect/HashBiMap.java#L461) consequently attempts to delete the old node, which is already absent. The colliding bucket traversal then reaches null. The [forward setter](https://github.com/google/guava/blob/f06690fa3e874f65515e8fd338a74d636e2c792f/guava/src/com/google/common/collect/HashBiMap.java#L545) explicitly repairs toRemove at lines 558-560; the inverse setter omits this repair.

Root's actual case reports two nested NullPointerException removal outcomes and a remaining oracle mapping: `size=1;F[v0a=k3a];I[k3a=v0a]`. The candidate drains to `size=0;F[];I[]`. The common assertion is retained. Classification is **ORACLE_CONTRACT_BUG**, not a bad test assumption and not behavior to reproduce merely to obtain a green JRE oracle run.

All source atoms are pinned to Guava commit `f06690fa3e874f65515e8fd338a74d636e2c792f`: Android body `ad9138c8eca437bda700df63a4afa80e5abe474f`, JRE body `f5254b367e72e97067739c40c91bdfd94edd4847`, identical BiMap API `9adbff493fad52a1d4b195ed5ad0b830c6b2542a`. Original headers and Apache-2.0 attribution remain required. The before-code FOSS gate and original port receipt are immutable and were not changed for this evaluation.

The original 01 summaries contained a misleading hard-coded JRE requirement label during the Android run. The discriminator now reports the actual HashBiMap CodeSource artifact basename. Only runtime provenance metadata changed; the 66 cases and assertions remain unchanged. Root will rerun both binaries and link the original 01 receipts as prior finite count/body evidence with this label correction.

Current admission is **Android donor finite-corpus fit only**. Full JRE parity and the rest of the requested collection-library implementations remain pending. This evaluation adds no production changes and makes no GC or hosted-CI claim.
