<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Filtered bulk admission and budgeted index-mask windows

This is a source-received increment in the existing independently built `m3/collections` SDK.
It is not a second collection hierarchy, a Guava/Eclipse/Spring/Commons namespace clone, or a
replacement of `java.util`. See `SDK_NAMES.tsv` for exact contract names and explicit exclusions.

## Existing owners enhanced

`M3MutableFilteredViews` retains its public factories and JDK NavigableSet/NavigableMap returns.
`addAll` and `putAll` now write exactly the references admitted in their single input traversal.
Sets use a temporary dynamic reference array; maps use separate key/value reference lanes. The
existing transient comparator-representative index is reused. Entries returned by a reused cursor
are captured immediately; comparator-equal aliases remain in encounter order, with the owner's
first stored representative preserved. No retained per-element wrapper/payload cache is added.

Predicate or input-enumeration failure precedes mutation. This is not an all-or-nothing transaction:
backing range, comparator, type or mutation failures can still leave the owner's usual partial result.
Objects are not deep copied, predicates must remain stable/nonmutating, and custom bulk overrides
that inspect the input wrapper's concrete identity/type are not qualified by standard-owner tests.
Bulk capture adds O(batch size) temporary references and can bypass TreeMap's sorted-input fast path;
no general allocation or throughput improvement is claimed.

`M3IndexMaskWindow.scanBounded` independently limits source ordinals examined and output slots written.
A call with no matches can return an empty window while advancing the cursor. Continue until
`nextIndex(result) == orderedRows.length`; do not confuse `written(result) == 0` with exhaustion.
The existing `scan` method still scans until it fills the requested output or reaches the source end.
Both preserve duplicates, signed row IDs, encounter order and caller output offsets.

```java
long result = M3IndexMaskWindow.scanBounded(rows, states, first,
        512, required, excluded, output, 0, output.length,
        M3IndexMaskCompactor.Backend.JAVA);
int next = M3IndexMaskWindow.nextIndex(result);
int count = M3IndexMaskWindow.written(result);
// The host checks cancellation/source generation before scheduling the next bounded call.
```

The native path extends the existing `m3_collections_mask` library. Original symbols are unchanged;
new bounded calls require the additive symbol. Fixed 128-element Region scratch never reads past
the admitted input budget. No predicates, comparators or object payloads cross JNI. The work limit
bounds loop iterations/primitive reads, not wall-clock latency. Inputs must remain stable during each
call and the host must reject stale generations between calls.

## Source and recipe custody

Target base: `hsoliwal/M3jdk21@4e29dc9d4141261e8763fbe9ffd6a20e017b1aff`.
Canonical recipe: `com.synexia.m3.SdkFilterWindow20261017`, task
`synexia-openrewrite-recipes/recipes/sdk-filter-window-20261017` in `hsoliwal/com.synexia`.
It reuses the exact existing `M3GpuToolJoinRecipe` / `M3GpuToolJoinManifest` family.
The receiver SOURCE_RECEIPT.tsv preserves previous digests as a dated successor; historical
source-receipt files and unmodified owners are not rewritten or silently grandfathered.

Run `python3 m3/collections/qualification/sdk-filter-window-20261017/verify.py` from a checkout.
It verifies all eight selected Java/test/C source hashes, compiles exact Java21 owners (no stubs),
runs the deterministic contracts, builds C17 with UBSan and runs real `-Xcheck:jni` parity.
`--java-only` explicitly reports native NOT_RUN. Full module authority remains
`mvn -B -ntp -f m3/collections/pom.xml test` with its approved dependencies, not a substitute POM.
The additive JUnit adapter runs the Java contracts; native is a separate required selected gate.

## Compatibility and remaining admission

Existing #1111 read-only navigation/facade consolidation, #1117 rank/select, #1118 long-path masks,
#1113 atomic selection, #1116 java.util occurrence repair and #1119 String bridge are not overwritten.
Full Maven/OpenRewrite SDK, owning-module/JUnit/source-inventory successor reconciliation, jtreg/JCK,
installed JDK, other native platforms and production performance remain separate gates. The active
STRING receiving phase is not advanced. No SWT/Nebula implementation is replaced by this SDK leaf.
Keep the review draft until those gates execute at the actual receiving head.

First-party Apache-2.0 copyright and notices retained. External libraries and challenge problems are
contract/category references only; no external implementation or editorial body was copied.
