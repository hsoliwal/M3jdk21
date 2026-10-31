<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3String regex pressure follow-up

This is the next source-sealed stage after M3JDK21 PR #894 at
`03b949881dd7ae140ab1e29cb97bf1af644a22c2` and Synexia PR #11421 at
`436f7705a179bc494d20e08c383c4f65499b9c9a`. Reusable authoring remains in Synexia:
`com.synexia.rewrite.M3Jdk21RegexPressureRecipe`.
The existing naming-map supplement selects this stage through `pressure_followup`.
Historical receiver evidence is retained; it is not automatically promoted to a full-runtime pass.

## Changes and unchanged ownership

`java.lang.M3StringRegexDp` now softly holds its existing derived-metadata cache.
Its entries still weakly key the canonical owner and exact coordinate; effects still require
identical plan ownership. No String, M3String, canonical-owner, payload or public JDK API field
is added. A strongly in-use cache may remain live until the current operation completes.

Before already-eligible hierarchical preparation, the helper samples Runtime maximum,
committed and free heap. The normalized headroom calculation cannot overflow. Preparation
suspends below max(512 KiB, maximum/32); a suspended helper resumes only at
max(1 MiB, maximum/16). A decline or caught optional-allocation OOME clears the derived root
and retains the previous rolling-DP/JDK fallback. There are no new caller properties, monitors,
threads, sleeps, forced-GC requests, native-memory reservations or systemwide services.

These are advisory heap snapshots, not coherent reservations or OS/cgroup/native-memory totals.
Soft-reference clearing timing is VM-controlled; it is not strict LRU and does not promise a
particular retained size at every instant. This stage does NOT spill or destroy canonical text.
Runtime/API references are Java 21 Runtime and SoftReference documentation. The existing
M3StringPool native admission remains a separate owner and is unchanged.

`jdk.internal.mindex.M3RegexDpPlan` stores 62 literal masks rather than128, using a perfect
index for the admitted ASCII letters/digits. It allocates only nonzero nullable propagation
levels (at most6). For the same64 plan/128 effect slots, the conservative retained array-payload
bound becomes169984 bytes rather than271360:101376 fewer bytes. Headers, references, scalars,
in-flight cache generations and transient working memory are excluded. This is a capacity
calculation, not a measured whole-runtime heap reduction.

## Java/JNI composition

Zero/singleton frontiers remain Java row selections. The original generic native
`compose_rows` function body is byte-identical to the previous stage. Its original JNI export
`Java_jdk_internal_mindex_M3RegexDpPlan_compose0` is retained.

A new package-private/native entry and export,
`Java_jdk_internal_mindex_M3RegexDpPlan_composeStructured0`, is additive ABI, not ABI identity.
Java selects it only for all-star programs with eligible multi-state rows. Its kernel validates
all dimensions and bits before touching output, recognizes zero-right relations, and proves
containment of nonempty right rows. If that proof succeeds, the lowest productive selected row
dominates the union. Otherwise it performs the exact generic union. The proof stops on its first
counterexample. Output remains alias-safe and no canonical text or retained heap pointer crosses JNI.

When only the older library is installed, missing structured JNI falls back to the original
export. With no library, composition falls back to Java. Both compatibility modes were executed.
Public descriptors of M3RegexDpPlan and Effect are unchanged; the native symbol set is explicitly
extended by one entry. The normal scalar String matching path and existing language/work limits
are not widened or replaced.

## Observed tests

Actual generated plan, patched into java.base, with a real JNI library and -Xcheck:jni:
90342 JDK matching cases,460314 split comparisons,2654316 arbitrary-frontier comparisons,
8192 generic and16385 structured matrix comparisons. Older-library and no-library modes
pass separately. All65536 UTF-16 units are checked or explicitly refused as surrogates;
62 literal lanes and nullable widths through64 states are checked.

Both C kernels pass16384 cases each, with three alias variants and invalid-row guards, under
ASan/UBSan and separately compiled portable bit iteration. JNI null/dimension/foreign-bit
boundaries, overflow, incompatible plans, cancellation and interrupt preservation are tested.

The wrapper uses explicitly TEST_ONLY text owners/cursors. Its12008 policy/control checks
include an independent BigInteger oracle, threshold hysteresis,512 concurrent calls and
manual cache clearing. In a real16 MiB Java heap, allocation pressure cleared the softly held
metadata before an induced OOME; after releasing test allocations, matching rebuilt metadata.
That VM/reference result is real; the text fixtures are NOT actual M3String storage or HotSpot
integration. The test-only recovery calls System.gc; the production helper does not.

The updated actual-target jtreg source compiles but has not run in a complete M3JDK image.
Optional metadata refusal or reclamation is not treated as a semantic failure by that test.

## Performance and rejected candidates

The initial generic property-scanning replacement regressed some dense cases and was rejected.
The original generic function was restored; the new proof kernel has a separate entry and
restricted selection. Recorded direct-C microbenchmarks retain all outcomes: they are not JMH,
JNI round-trip, String.matches, or full-product measurements. In one seven-sample64-state run,
structured regex-suffix composition measured120 ns versus1464 ns for the baseline generic kernel.
Other generic measurements varied or regressed despite identical function text. No general
speedup or zero-overhead claim follows; product performance qualification remains open.

## Still pending

Maven/OpenRewrite SDK execution, complete source reactor, full M3JDK/libjava build, real
canonical-storage jtreg with M3/CompactStrings variants, Windows/MSVC, retained heap/native
profiles and end-to-end latency remain pending. The heap policy has no strict LRU or payload
spilling guarantee. Full regex parsing/captures/find/restart/images, compiler lowering, static
reference-count claims, the whole donor inventory, DOOP viewport work and homepage terminology
are not implemented by this receiver. Keep these as open work, not fabricated completion.
