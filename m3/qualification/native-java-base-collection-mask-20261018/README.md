# M3JDK21 real java.base collection-mask receiver — October 18, 2026

**M3JDK21 is the JDK**, not a JDK-plus-M3 adapter. This PR modifies the actual `java.util.ArrayList` and `java.util.Vector` implementation in `src/java.base` with one canonical `jdk.internal.mindex.M3CollectionMaskCompactor`.

## Source-first custody
- First-party Synexia recipe PR [#12284](https://github.com/hsoliwal/com.synexia/pull/12284) contains the Maven/OpenRewrite `M3HashPinnedJavaSnapshotRecipe`, two EXACT Java21 product preimages, three ABSENT-before sources and five postimage templates. The generated product files here are byte-identical Git blobs. No generated source was hand-edited in this receiver.
- Product master baseline: `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`. The two product preimages were read back and matched the exact Synexia recipe pre-blobs. Every addition to the JDK product is subject to source-hash drift refusal.
- Retain GPLv2 with Classpath exception on all `java.base` JDK sources; original OpenJDK notices unchanged. Algorithm catalogue/recipes are first-party Synexia Apache-2.0, no third-party solution bodies copied.

## Original JDK semantic contracts
- `ArrayList.removeIf(Predicate)` and its backed `subList.removeIf` retain original predicate visitation order, short-circuit run until first removed element, CME only after predicate traversal, `modCount` semantics, discarded tail nulling, and parent/sublist bookkeeping.
- Vector `bulkRemove` (also used by `removeAll`, `retainAll`) preserves original synchronized block, exception rollback, modification detection, and clearing of garbage references.
- No direct raw Java object pointer crosses JNI; copies of `Object[]` use `System.arraycopy` and the VM's GC barriers. Existing optional primitive JNI selection/ordering in the M3 Collections domain remains separate and unchanged.
- For spans below 512 or with dense/interleaved removal mask runs the existing scalar compaction is preserved. Large long survivor runs may use `System.arraycopy`; do not claim a universal performance win before A/B metrics.

## Testing and admission
Product CI uses pinned Java21 to copy only the three reviewed production files into a temporary `java.base` patch source tree. It compiles them and runs the two exact committed JDK regression oracles against the actual patched VM using `--patch-module`. It also validates binary API declarations and source SHA-256 seals, tests GC/identity/sublist/synchronization boundaries, and runs a bounded optional JNI primitive regression as a separate phase when supported.

Standalone Java21 proofs prior to this receiving commit: **13,493,171** object-mask parity checks; **283,701** backed ArrayList operations; **2,644,984** Vector operations. These were independent harnesses, not a complete patched-JDK build. The complete modified HotSpot/JCK/jtreg/module-rebuild/release throughput gates remain separate.
