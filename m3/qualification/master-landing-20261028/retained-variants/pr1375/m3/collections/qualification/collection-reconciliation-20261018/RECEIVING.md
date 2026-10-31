# M3JDK21 existing-owner collection reconciliation — 2026-10-18

This receiver combines PR1328 and PR1366 with the exact PR1375 parent
fcf3965eb7e021560aa49bbc722fd2edf4abb1b7. It is not a second collection hierarchy
or a service layered over the JDK. The canonical source/recipe packet is
https://github.com/hsoliwal/com.synexia/pull/12386 at
7fafc4e3c9b9c7ab2b5d942768564ac865be3074, following documentation commit
21bb039f88406a105ce70f1ff8e745ac7503c0b6. Task:
synexia-openrewrite-recipes/recipes/jdk21-packed-filter-20261018/reconciliation-20261018/.

Four existing production files change. M3FilteredViews receives the stored-null
atom while retaining fused terminal loops. M3IndexPathMasks equals PR1328's
mask/output alias-refusal postimage. M3IndexedListSnapshot equals PR1366's packed
occurrence-filter postimage. Native compactor combines PR1375 input-prefix scratch
reuse with PR1366 second-pass empty-selection verification. Both native composition
orders produce identical bytes. All other PR1375 files remain inherited unchanged,
including bounded-window transfer and module-pack header-derived build admission.

Retain live versus detached-view distinctions, Object[] payload identity, int[]
occurrence indexes, from/to ranges, stable indirect sort, nulls, duplicates,
callback order and exception identity. No public/JNI signatures, native loader,
per-entry structural object, source-owner hierarchy or module descriptor changes.
Only the two reproduced baseline contract defects intentionally change results.

## Source custody and fresh execution

M3Jdk21CollectionReconcileRecipe reuses unchanged M3GpuToolJoinRecipe/Manifest.
Ten source-sealed inputs: eight generated changes plus two KEEP co-inputs.
Template tree530d07dc1e4d60df42139786adf6ba019c9fe234, before tree
617a711cea049d2c8d4fc41b85f6516fa675b089. No emitted target file was hand-repaired.
Actual manifest kernel1079 initial/1089 replay checks includes all1024 mixed
before/post states and refusal/fixed point. Actual Maven exited127: mvn not found.
Kernel is NOT OpenRewrite SDK execution; SDK and JUnit discovery remain unqualified.

Fresh serial source qualification used four complete Java owners, no API stand-ins.
New corpus4805748 Java checks at32m/128m and4805749 native-mode checks, digest
9707939200898070008. Existing packed corpus351161 Java checks per stage/interpreter
repeat and11879132 JNI checks per original/original, combined/original and
combined/combined run. Two real native libraries, eight unchanged header-matched
exports, strict C17, UBSan and -Xcheck:jni. Nineteen compiled public/protected type
descriptors match. Both baseline defects reproduce; two compiled wrong variants
each fail twice with the same fingerprint. All27 frozen source inputs remain exact.
Counts repeat finite corpora; these are not full-module/image acceptance receipts.

## Resource boundary

Same-compiler int/long native frames remain1408/2432 bytes, preserving1375.
32 all-selected snapshots retain32 index directories before versus1 after;
17-slot partial filters retain17 slots. Explicit-GC heap deltas are noisy and
include a negative candidate observation, not negative allocation. Java allocated
bytes per call at1048576 rows decrease in all four measured densities. Native CPU
is mixed across two process pairs (second order reversed): first half-selection
1873889.6->2218041.55ns/call; second empty case1594728.6->1599206.25ns/call.
Adverse results are retained. No universal speedup, JMH or performance promotion.

## HOLD

Actual SDK, full owning collection module, installed M3JDK21 image, jtreg/JCK,
HotSpot/GC/JIT, other native platforms and SWT/Nebula/web/Skija/token-regex integration
remain NOT_RUN. Parent1375 historical linked-image evidence was not rerun for this
combination. COVERAGE.tsv keeps strict reversed snapshots and incompatible token
traversal/match-selection contracts separate. String-first receiving phase unchanged.
No default branch, root policy, existing PR, merge, rebase or force push is changed.

Runtime blobs:
M3FilteredViews cda44496ad9dec48a77e1f922fba3b23082c92df
M3IndexPathMasks 3875db690a2519975c531b33519a4a1243c24f2d
M3IndexedListSnapshot e69da823d4db909bbd99fb94cb6002d5bff5025f
m3_index_mask_compactor.c 49e8bac26e4017a262f18f202298350239f1133b

Full original/template/proof sources, build/test logs, raw resource samples and
reproduction scripts live in the canonical packet and delivery archive. Target
SOURCE_SEALS.json pins pre/postimages; rerun the canonical recipe on source drift,
never overwrite an incompatible later target version with this snapshot.
