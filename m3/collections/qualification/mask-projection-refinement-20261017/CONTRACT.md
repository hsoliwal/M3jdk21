# Mask-driven projection refinement

SPDX-License-Identifier: Apache-2.0
Copyright 2026 Hitesh Soliwal and contributors

## Authority and scope

Target: hsoliwal/M3jdk21 master d47a4b4fdb8e3794ec56768b06aa9e23730c6b8e.
Only M3LongIndexProjection.java and collections/m3_index_mask_compactor.c runtime
internals may change. Existing public/protected interfaces, retained fields, enum values,
JNI symbols and ownership remain locked. This is a bounded Apache m3/collections candidate,
not promotion into java.util, completion of the full reactor, or advancement of the active
String-first phase. Synexia remains the reusable convergence/recipe owner; M3JDK21 owns
its target runtime, naming, native ABI and acceptance. No Synexia runtime dependency.

## Representation invariants

Collections are existing primitive payload arrays plus ordered physical index vectors,
half-open offset ranges, and aligned unsigned-byte state masks. State masks are application
metadata, not Java GC reachability or synchronization facts. No per-entry wrapper/node,
second payload owner, global scheduler or probabilistic equivalence is introduced.

M3LongIndexProjection already privately owns immutable values and indices. Full-index views
may lend the existing immutable index to M3IndexMaskCompactor because that owner neither
mutates nor retains input arrays. Offset subviews retain the defensive range-copy path.
Filtering still invokes the selected backend after the same null/length/mask checks; empty
and all-selected native inputs must not bypass provider validation. Returned row order,
duplicate occurrences, original physical indices, slice offsets and enum order are unchanged.
Explicit physicalIndices()/toArray() remain defensive materialization boundaries.

A view certified ASCENDING or DESCENDING may satisfy a repeated same-direction sort by a
new view over its existing immutable arrays and range. ENCOUNTER is not a sortedness proof.
Opposite-direction sorting must still run the stable signed-long index merge, never reverse
a vector because reversing would reverse equal-value occurrence order. A sliced sorted view
retains its order certificate. Every returned wrapper stays a distinct object.

Native mask selection retains two state passes and the 256-element JNI Region boundary.
The second pass computes an invocation-local 256-bit selection mask before row-ID transfer.
A rejected chunk need not transfer row IDs; a full chunk may write the row-ID buffer directly;
a mixed chunk compacts selected IDs in increasing source ordinal. No native array pointer
survives a call. No callback, global state or new native allocation is introduced. Count drift
is still checked before output writes and at termination. Partial unpublished results never
become public success; source stability remains a caller obligation. Zero masks, unsigned
bit7, duplicate/negative row IDs, tails and empty input retain exact values and validation.

## Recipe and source custody

Reuse the canonical M3GpuToolJoinRecipe/Manifest source-sealed Maven/OpenRewrite implementation,
with exact Apache-2.0 source identities from Synexia PR12019 head
287afcdcd2dbcff5a43ce98368b14d86e182d16c. A small target binding and proof mirror are allowed
by m3/docs/m3jdk21-porting-invariant.md EXTERNAL_RECIPE_TOOL; they are not a new canonical
engine or runtime library. Qualify named recipe, partial states, drift/missing/duplicate,
fixed points and opaque payloads before materializing actual SDK Results. Fix templates,
not outputs. Preserve original target regression source without editing it.

Runtime gates: patch -> source/native lint -> Java/native compile -> Java tests -> actual
JNI -> replay. Apply files serially with per-file receipts. Bind pre/post SHA-256 and Git
blobs, public/field descriptors, source order, exceptions and native symbol. Keep negative
controls and all failed stages. Finite corpora are not universal semantic-equivalence proof.

## Catalogue and mapping

The existing target M3LongIndexProjection and M3IndexMaskCompactor remain canonical names.
A task-local mapping receipt explicitly excludes central rename-map edits: no name or ABI
moves, and no src/java.base port is admitted. Add category/source/license/complexity/selection
records to the existing m3/collections/algorithm-catalog namespace rather than another catalogue
engine. LeetCode stable compaction, HackerRank bit-array and GeeksforGeeks bit-count categories
are references only; no challenge/editorial implementation bodies are copied. Existing target
and Synexia recipes/mechanics are reused with original notices and exact source seals.

## Acceptance boundaries

Report measured allocations, JNI transfer counts and execution times separately. Skipping
transfers does not establish general JNI speedup; added bit work may regress dense cases.
Full JDK build, jtreg, module/reactor integration, canonical broader inventory/provenance,
Windows/macOS, workload/JMH and hosted CI remain explicit pending gates until executed.
No root/default-branch writes, merges, force pushes, workflow weakening or hidden collection
replacement. Documentation here is the frozen proposal, not evidence of an executed gate.
