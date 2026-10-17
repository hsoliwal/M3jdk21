<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Stable groups, indexed mask windows and nullable filtered membership

Target: M3JDK21 independent Apache collections module. Base: PR #1110,
commit `563e5b303e1c38fc9e3ed4098eac1b707a90398a`; preserve its offset-aware
native filtering, legacy-binary behavior and same-direction index reuse.
Synexia remains the convergence/recipe owner. No standard java.util interface is renamed.

## Contract by owner

M3LongIndexProjection retains one immutable primitive payload, a primitive physical-index
array and a half-open logical window. Known opposite-direction sorting reverses whole
value groups while copying each group's physical IDs forward. Equal-key occurrence order
is NOT reversed. Unknown encounter order retains existing stable signed-long merge sort;
same order retains the preceding PR's shared immutable directory and distinct descriptor.
Slices and filters keep their exact offsets, masks, payload identity and detached-snapshot
contract. No per-row objects, payload copy or new retained cache is introduced.

M3IndexMaskWindow still accepts unsigned-byte required/excluded flags aligned by logical
source ordinal, not physical row ID. All geometry/alias validation and explicit native
availability occur before shortcuts. A zero limit returns the original cursor; contradictory
masks select nothing and return source end; zero masks copy an ordered bounded range.
General JNI scan builds a transient 128-bit decision mask for each existing 128-entry region.
All-rejected regions need no row-ID read. Accepted rows compact into the same local prefix;
no source/output alias, callback, critical pin, native heap, retained pointer or ABI change.
A returned cursor stops just after the last inspected entry needed to fill the output.
Concurrent mutation of supplied arrays is outside the existing stable-input contract.

M3FilteredNavigableSet remains a read-only live wrapper. Its comparator and backing owner
remain authoritative. A null ceiling is ambiguous on comparators supporting null values;
the fallback iterator resolves the stored representative before invoking the predicate.
This deliberately repairs the original contains(null)/iteration inconsistency, not a claim
of byte-for-byte behavior equivalence for that defect. Unchanged normal membership, iterator
preparation, read-only mutation refusal and live predicate/backing behavior are regression tested.
Bounded-view navigation outside the backing range and complete wrapper-family closure remain
separate pending work; this patch does not silently claim those solved.

## Recipe and proof

Canonical recipe: `com.synexia.rewrite.M3CollectionsOrderWindowRecipe` with resource root
`/com/synexia/rewrite/collections-order-window-20261017/`, in com.synexia.
Source-changing work improves its sealed templates, then replays the unchanged canonical
M3GpuToolJoinManifest and M3SpanRecipeProof into a fresh candidate image. Template/preimage
SHA-256 and exact original Git blobs are retained. Each C/Java source pass compiles and runs
its applicable oracle; changes to product outputs outside that recipe are not admitted.

Run the canonical task `synexia-openrewrite-recipes/recipes/collections-order-window-20261017/verify.py`.
Its Maven POM invokes the same proof and includes a class-backed OpenRewrite recipe.
Executed source-seal/Java/JNI evidence is distinct from unexecuted Maven/SDK/JUnit/full-module
and complete JDK image gates. Keep the receiving PR draft until those gates complete.
The ordinary String/regex/DB work elsewhere in this chat is not marked complete by this patch.
