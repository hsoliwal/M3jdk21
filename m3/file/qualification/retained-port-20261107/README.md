<!-- SPDX-License-Identifier: Apache-2.0 -->
# retained-port-20261107 (m3/file)

Port of the retained production code of `m3/file` (input `S:/m3-scratch/eb-conv/folddrop/retained/m3__file.tsv`:
103 rows from the retain merges of PRs #1538, #1590, #1608, #1611, #1619 and #1975; 60 distinct files, 67 distinct
blobs) onto today's `jdk.m3.file`. A byte restore of all 103 rows does not compile (66 javac errors in 13 files): the
donor entry facade `M3File` (path/id/tree/observe) is not in the retained list and the name `jdk.m3.file.M3File` is the
MIndexFile port, the donor `M3FileSource` interface collides with the byte-image class `M3FileSource`, and the donor
progress/snapshot/delta types evolved into `M3StreamProgress`, `M3NioFile` and `M3NioFileDelta`. Every file therefore
carries one decision in `DECISION.tsv`; only RESTORE_EXACT and ADAPT rows are written.

| file | content |
|---|---|
| `DECISION.tsv` | one row per input row (103): RESTORE_EXACT 25, ADAPT 1, SUPERSEDED 21, HOLD 56; per distinct file 15 / 1 / 15 / 29; master owner and evidence per row |
| `RESTORE.tsv` | the 21 files apply.py writes (16 sources, 5 tests): source blob (JDK-named master copy of the retained PR head under `m3/qualification/master-landing-20261028/retained-variants/pr<pr>/`), parent blob and link, rule, pinned sha256 |
| `EDITS.tsv` | the one anchored insertion into an existing owner: `M3FileSource.byteAt(int)`, a member of the #1975 parent dropped from master, before/after sha256 |
| `apply.py` | recipe RETAINED_PORT v1: git-id-verified blobs, package rule, ADAPT rule, one `//FLLDCIM` anchor per file, hash pins, `--check` fixed point, refuses drift (exit 2), idempotent |
| `differential.sh`, `differential/ShadowDifferential.java` | seeded donor-vs-head differential of the 12 restored observer atoms (JIT and -Xint, byte-identical output) |
| `mutants.py` | one mutant per adapted or edited class, replayed through the ported tests (JUnit console launcher / contract main) |
| `GATES.tsv`, `RECIPE_MEMORY.tsv` | gates with numbers; corrections made while building the recipe |

## Decisions

**RESTORE_EXACT (15).** The shadow observer stack of #1590/#1619: `M3FileShadow` (byte observer, the shape of
`M3NioFile.ByteObserver.onBytes`), `M3FileCharShadow`, `M3FileUtf8Shadow` (split-safe UTF-8 char layer),
`M3FileLineShadow`, `M3FileTokenShadow`, `M3FileCrcShadow`, `M3FileDigestShadow`, `M3FileTextShadow`,
`M3FileWcShadow`, `M3FileStructHash`, `M3Grammar` and `M3Paragraphs` (12 files, a self-contained closure with no
master owner: `M3FileStreamIndexer` is the M3File admission pass, `M3TextMetrics` UTF-16 text facts). `M3ModuleResources`
(#1538, module-reader resource scope, no master owner). `M3FileEncodedEdits` and `M3FileTextCoordinates` (#1975, layers
over the master owners `M3FileSource` and `M3FileEditBatch`; they need `M3FileSource.byteAt(int)`, restored by
`EDITS.tsv` from the same parent). The restored observers are driven through `M3NioFile.observe(long, M3StreamProgress,
ByteObserver)`; their javadoc still links the superseded `M3FileShadowInputStream` plumbing.

**ADAPT (1).** `M3FileScope` (#1608): whole-word `M3File -> M3NioFile`; the donor interned the `M3File` handle by Path
through `M3File.of(Path)`/`path()`, the master handle is `M3NioFile` with the same members. `M3FileScopeTest` is ported
with the same rule.

**SUPERSEDED (15, owners cited per row).** #1611 entirely: `M3ShadowInputStream`/`M3ShadowOutputStream`/
`M3StreamObserver` -> `M3FileStreams` + `M3ShadowStreams` (file-unify-20261106); `M3FileTraversal` (1611 blob) ->
`M3FileUniverseTraversal`; `M3FileFact*`, `M3FileIndexedSnapshot/Delta` -> `M3NioFileDelta`. From #1590/#1619:
`M3FileShadowInputStream` -> `M3NioFile.observe` (+ `M3ShadowStreams.monitoredInput`, `M3FileStreams.input`),
`M3FileShadowOutputStream` -> `M3ShadowStreams.output`, `M3FileLimitException`, `M3FileProgress`/`M3FileSubProgress` ->
`M3StreamProgress` and the owner `jdk.m3.collections.M3ProgressMonitor` (progress-monitor-unify-20261106),
`M3FileTaskWindow` -> `jdk.m3.collections.M3KeyedParallelismPlan` (its contract is red on this machine with the donor
class itself: cancellation, `M3FileTaskWindowContract.java:137`).

**HOLD (29).** The 27 tree-lane files of #1590/#1619 (`M3FileTree`, builder, scanners, delta, walk, cursor,
spliterators, traversal, parallel, residency, names, flags, kinds, errors, hashes, archive/zip sources and writer,
word count, grep match and shadow): their hub, the donor entry facade `M3File` at 0cd0944136c1, is not in the retained
list and its name is taken; the landed other half is `m3/fs` `M3DirectoryTable`/`M3HostFileInventory` (#2663) with
`M3FileUniverseTraversal` and `M3KeyedParallelismPlan`; renaming the facade or folding the lanes onto them is a design
decision. `M3FileObservation` and `M3FileVersionView` (#1608): their producer `M3File.observe()`/`snapshot(int)` is not
retained and 9 of 11 donor test methods need it.

## Tests

Ported from the parent commits where they compile: `M3GrammarContract` + `M3GrammarTest` (1619),
`M3FileEncodedEditsContract` and `M3FileTextCoordinatesContract` (1975, run by the new wrapper
`M3FileRetainedContractsTest`, the pattern of `M3GrammarTest`), `M3FileScopeTest` (1608, adapted). Surefire: base
36 run / 1 failure, head 43 / 1 (the pre-existing `M3FileDeltaTest.fullSourceClassAndCliContract`). Not ported:
`M3FileObservationProviderTest`, `M3FileShadowTest`, `M3FileShadowBoundary*`, `M3ModuleResourcesSelfTest/Test`,
`M3FileTaskWindowContract` (`GATES.tsv` row 6).

## Replay

`python -I apply.py --check` (exit 0, changed=0); `mvn -B -ntp -T1 -f m3/file/pom.xml clean package
-Dmaven.test.failure.ignore=true` (red at the python3 exec step on this machine, as master);
`bash differential.sh m3/file/target/classes <out> 3000 1381257296 <jdk-21>` (DIFFERENTIAL PASS, sha256 fc4b2ad4580c);
`python -I mutants.py <root> "<m3-collections jar>;<m3-arrays jar>;<junit jars>" <junit-platform-console-standalone>
<out>` (2/2 killed). Numbers in `GATES.tsv`.
