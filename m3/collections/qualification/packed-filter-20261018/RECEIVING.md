# M3JDK21 packed filter receiving

Canonical recipe: https://github.com/hsoliwal/com.synexia/pull/12368
Exact source commit: 6a2320831fc6a88bd2b96052734d1e011b3d1d9a.
Docs-first commit: 9974b7618f89f9445b5abb5699eb647db224677e.
Target baseline: 2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49.
Crate: synexia-openrewrite-recipes/recipes/jdk21-packed-filter-20261018/.
Class-backed Maven recipe: com.synexia.rewrite.M3Jdk21PackedFilterRecipe.
Existing M3GpuToolJoinRecipe/Manifest unchanged; source remains canonical in
Synexia, not an M3JDK21 runtime dependency. Complete preimages, templates,
recipe tests, verifier, resource/fault fixtures and evidence remain at that pin.

Changes are within two existing M3JDK21 production owners: snapshot Java filtering
and the native compactor zero-result path. No new public method, native symbol,
payload owner, per-entry Node/Entry, collection hierarchy or widget wrapper.
Live filtered views remain live; this snapshot remains detached. Stable sorting
and existing Object[]/int[]/from/to storage remain authoritative. Complete immutable
all-selected directories are shared; partial slices still copy only their range.
Native nonempty loops stay unchanged after a broader prefix-read variant was
rejected for mixed-density CPU regression. Original native validation, loader,
byte-state ABI and no-callback rules remain.

Actual isolated source qualification: Java351161 checks per process at32m/128m
before/after (digest13045526194583062665); JNI11879132 per original/original,
candidate/original and candidate/candidate combination (digest17855792429146116619).
Strict javac21/C17, generated header, seven symbols, UBSan, -Xcheck:jni. Public/
protected descriptors unchanged. Kernel165/172 checks includes all128 mixed states,
refusal and fixed point. Missing-native3 checks; two compiled wrong variants rejected.
32 complete all-selected snapshots:32 index directories before,1 after; partial
filtered slice17 slots on both. Source and detailed evidence identities accompany
the canonical crate and archive. Counts are corpus assertions, not full JDK tests.

Maven actually exited127: mvn not found. Kernel replay is NOT SDK execution.
Maven SDK/JUnit discovery, full collection module, installed JDK, jtreg/JCK,
HotSpot/GC, other native platforms, SWT/Nebula/web/Skija and application performance
remain HOLD. Mixed-density CPU is not uniformly improved despite lower measured
allocation. No universal speedup. #1260/#1328 and the earlier widget source stacks
remain separate pending reconciliation. String-first receiving phase unchanged.

Qualified product blobs:
M3IndexedListSnapshot.java e69da823d4db909bbd99fb94cb6002d5bff5025f
m3_index_mask_compactor.c 09e0dc2a4e0bde839ddf2b8fd0cc2d658ee59489
SHA256 pre/postimages are in SOURCE_SEALS.json. Reapply only canonical recipe
output to the exact admitted inputs; no target hand-repair, merge, rebase,
force push or default-branch write.
