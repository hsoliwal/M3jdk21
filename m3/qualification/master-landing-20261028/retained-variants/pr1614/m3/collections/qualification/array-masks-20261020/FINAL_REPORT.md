# Collection array/mask receiving result

## Files changed

Two existing M3JDK21 production owners and four generated regression/probe sources. M3ArrayList.removeIf now uses lazy offset-relative long[] masks with private marking/compaction atoms. M3IndexedListSnapshot.sortedMaskedByPhysicalState sorts its unexposed freshly selected int[] directly, reusing the existing sortPositions atom and filteredSorted ownership pattern. No wrapper hierarchy, runtime Synexia dependency, public API or native export was added.

## Exact source and recipe

Product parent: 065e3626aac2de62d2f531aec3bed2fca533c9f9. Canonical Synexia commit: f530b310ae87c34a21e55b53a37b7fed4143d360. Task crate: synexia-openrewrite-recipes/recipes/keyed-api-20261015/collections-array-masks-20261020. Reuse M3GpuToolJoinRecipe/M3GpuToolJoinManifest; the target receives only exact emitted outputs and target receipts. PROVENANCE.tsv records every product preimage/postimage SHA-256.

The canonical crate retains immutable baselines, eight actual scheduler tests, category decisions, improvement memory, Maven logs, runtime command/output records and resource logs. Source drift, partial states, foreign occupants, corruption and cancellation are tested. The final default and explicit-output runs each passed all eight tests; generated source bytes match the runtime-qualified images.

## Verification executed

Diff, source hygiene and strict C syntax lint preceded Java21 compilation. Four current production classes and four probe/test sources compiled in each original/candidate image with warnings as errors. The separate focused component POM ran two JUnit tests per image with no failure, error or skip. This is not the original whole-module POM or its source-built JUnit provider.

Sixteen runtime launches compared both original/candidate images in default, interpreted, C1 and C2 modes. Removal: 597398 checks per launch, digest 5008654329505655679. Masked sort through actual JNI: 39612 checks, digest -2208568637010835148. All paired outputs match. Four public/protected descriptor comparisons and two generated JNI headers match. The unchanged current path-mask C17 selector compiled with fatal UBSan and ran under -Xcheck:jni.

## Measured resources

On this VM, no-match removal allocated 48 -> 0 bytes/call; 4096-element tail-only removal 576 -> 24. Half-selected masked sorting at size4096 allocated 24832 -> 16584 bytes/call. These are ThreadMXBean transient allocation observations, not retained-heap or RSS proof. Timing results are mixed, including slower removal scenarios; all raw observations remain in the canonical resource log. No universal speedup or peak-performance claim.

## Blockers and remaining scope

Original whole collection-module tests with the required source-built provider, complete source-built JDK image/jtreg/JCK, cross-platform JNI and hosted acceptance remain pending. This finite pass does not complete all collection forks, general LeetCoder coverage, all-method atomization, patternization or IOP. It does not advance the locked receiving phase. Keep draft; no merge performed.

An intake hash check rejected stale archived dependency bytes before compilation. Current Compactor and PathMasks inputs were restored and verified against their exact current Git blobs; archived test results were not reused as current proof. Full raw receipts and source closure are retained in the portable session delivery.
