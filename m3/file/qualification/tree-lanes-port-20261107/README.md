<!-- SPDX-License-Identifier: Apache-2.0 -->
# tree-lanes-port-20261107 (m3/file)

Second pass over the 29 files that `m3/file/qualification/retained-port-20261107` (#2770) HELD under "tree-lane family +
M3FileObservation/M3FileVersionView: API drift" (56 input rows of its `DECISION.tsv`: the 27 tree-lane files of PRs
#1590/#1619 and the two #1608 files). Every held file carries one decision in `DECISION.tsv`; only RESTORE_EXACT and
ADAPT rows are written, from the JDK-named master copies of the retained PR #1619 head under
`m3/qualification/master-landing-20261028/retained-variants/pr1619/`. No existing file is modified.

| file | content |
|---|---|
| `DECISION.tsv` | one row per held input row (56): RESTORE_EXACT 8, ADAPT 2, HOLD 46; per distinct file 4 / 1 / 24; master owner, evidence with whole-word reference counts, and the exact design decision each HOLD needs |
| `RESTORE.tsv` | the 5 files apply.py writes: source blob (retained-variants copy), parent blob and link, rule, pinned sha256; comments carry the held owner's blob of the hoisted constant and the base tree |
| `EDITS.tsv` | empty by design (no existing owner is edited); keeps the replay shape of retained-port-20261107 |
| `apply.py` | recipe RETAINED_PORT v2: git-id-verified blobs, package rule, ADAPT rule `HOIST_DEFAULT_RESIDENCY` (value re-read from the held owner's blob), one `//FLLDCIM` anchor per file, hash pins, `--check` fixed point, refuses drift (exit 2), idempotent |
| `differential.sh`, `differential/TreeLeafDifferential.java` | seeded tree-corpus differential: the retained PR #1619 module snapshot (47 sources, compiled whole) vs today's module with the ported leaves; JIT and -Xint; byte-identical output |
| `mutants.py`, `MUTANTS.tsv` | one mutant of the adapted class, killed by the differential driver against the donor output |
| `MEMDIFF.txt` | member-level check with `m3/qualification/foldaudit2-jbdrop-4d-20261107/memdiff.py` (closest master owner per file) and a same-name member scan over every master `src/main` of m3/file, m3/fs and m3/collections: no member of the 5 ported files exists on master under another name (no PRESENT_EVOLVED) |
| `GATES.tsv`, `RECIPE_MEMORY.tsv` | gates with numbers; corrections made while building the recipe |

## Decisions

**RESTORE_EXACT (4).** `M3FileFlag` (the 13 row-flag lane names), `M3FileKind` (FILE/DIRECTORY/SYMLINK/OTHER,
`bit()`/`mask()`/`of(BasicFileAttributes)`), `M3FileNames` (name-segment interner: open-addressed hash over one char
pool, SoA offsets, cross-table compare) and `M3FileWordCount` (files/lines/words/bytes/failed record): the typed atoms of
the tree family with no code dependency on the held hub; each compiles alone (javac 21 `-Xlint:all -Werror` probe) and
has no master owner (`M3NioFile.Metadata` carries booleans, `M3FileDictionary`/`M3FileLexicon` intern content tokens,
`M3FileWcShadow` counts one stream). `M3FileNames`' constructor and `intern` stay package-private until the tree owner
lands.

**ADAPT (1).** `M3FileScanSpec` (depth/residency/archive policy record): its only missing symbol is the constant
`M3FileTree.DEFAULT_RESIDENCY` (2 references; `M3FileTree.java:35` of the #1619 head, value 4096, held). apply.py hoists
it into the spec as `DEFAULT_RESIDENCY` (MRL constant hoist; the value is re-read from the held owner's retained blob at
apply time and the rule refuses when the declaration is not found exactly once) and rewrites both references. `javap -p`
donor vs head differs by that one added field.

**HOLD (24).** Design decisions, stated exactly in `DECISION.tsv`:

* The 21-file closure of the held owner `M3FileTree` (`M3FileTree`, `M3FileTreeBuilder`, `M3FileTreeScanner`,
  `M3FileTreeDelta`, `M3FileWalk`, `M3FileCursor`, `M3FileSpliterator`, `M3FileIdSpliterator`, `M3FileTraversal`,
  `M3FileParallel`, `M3FileResidency`, `M3FileHashes`, `M3FileChange`, `M3FileChangeProvider`, `M3FileMatch`,
  `M3FileGrepShadow`, `M3FileArchiveContent`, `M3FileZipScanner`, `M3FileZipSource`, `M3FileZipWriter`,
  `M3FileSystemSource`) needs one decision, after which the port is a whole-word rename recipe (the retained module
  snapshot compiles standalone: 64 class files, javac 21 `-Xlint:all -Werror`): (1) a name for the donor entry facade
  `M3File` of the #1619 head (231 lines, not in the retained rows; `jdk.m3.file.M3File` is the MIndexFile port): 58
  whole-word references in 15 held files; (2) a name for the donor source interface `M3FileSource`
  (openContent/metadata/openChannel/openOutput/separator/close; `jdk.m3.file.M3FileSource` is the byte-image class): 12
  references in 3 files; (3) the progress contract: 29 `M3FileProgress` references in 8 files and 20
  `InterruptedIOException` sites in 6 files keyed to `M3FileProgress.checkCanceled()`/`split()`; `M3FileProgress` and
  `M3FileSubProgress` were SUPERSEDED in #2770 by `M3StreamProgress`, whose contract differs (no begin/done/split;
  `checkCancelled` throws `CancellationException`): re-land the two superseded files for the lane or rewrite the
  cancellation contract (not MRL); (4) `M3FileParallel` needs `M3FileTaskWindow` (6 references; superseded by
  `M3KeyedParallelismPlan`, donor contract red), the facade needs `M3FileShadowInputStream`/`M3FileShadowOutputStream`
  and `M3FileLimitException` (superseded). Alternative: fold the lanes onto `m3/fs` `M3DirectoryTable`/
  `M3HostFileInventory` (#2663), which own none of the tree walk / zip scan / zip write / tree delta / grep capabilities
  (git grep over master `src/main`: `ZipOutputStream` 0 owners, `walkFileTree` only `M3FileWatcher`, `Spliterator.OfInt`
  only `jdk.m3.collections`).
* `M3FileError`: one code reference (`line 37`) to the superseded `M3FileLimitException`; master signals the byte limit
  with a plain `IOException("resource exceeds configured read limit")` (`M3NioFile.java:97,102,320,345`). Decision: give
  the master limit a type (then `M3FileError` adapts by one whole-word rename) or drop the `LIMIT` classification.
* `M3FileObservation` + `M3FileVersionView` (#1608): the class adapts by the #2770 rule `M3File -> M3NioFile`
  (`M3NioFile.Metadata` has the identical shape), but its constructor is package-private and the producer
  `M3File.observe()` of the #1608 head (`M3File.java:55-77`: attributes, `M3FileVersionView` lookup with
  NOFOLLOW_LINKS, `unix:ctime`, re-read and version bracketing) is not retained. Decision: add the bracketing producer
  to the master owner (`M3NioFile.observe()` no-arg) or drop the lane; all 11 `M3FileObservationProviderTest` methods go
  through it, so no test is portable without it. `M3FileVersionView` compiles alone but is held with its only consumer.

**SUPERSEDED (0).** No held file has a master owner of its capability (the #2770 SUPERSEDED rows already cover the
stream, progress, task-window and 1611 traversal/fact files).

## Tests

None of the 10 PR #1619 tests compiles standalone (every one drives the held facade `M3File.of(...)`/`M3FileTree.open`:
`M3FileTreeTest`, `M3FileTreeDeltaTest`, `M3FileWalkTest`, `M3FileZipTest`, `M3FileParallelTest`, `M3FileChangeTest`,
`M3FileErrorTest`, `M3FileNamesTest`, `M3FileOpsTest`, `M3FileLazyTest`), and `M3FileObservationProviderTest` needs the
held producer in all 11 methods. The executed proof of the 5 ported leaves is the seeded tree-corpus differential (gate
7) and the mutant pass (gate 8). Surefire is unchanged: 43 run / 1 failure (the pre-existing
`M3FileDeltaTest.fullSourceClassAndCliContract`).

## Replay

`python -I apply.py --check` (exit 0, changed=0); `mvn -B -ntp -T1 -f m3/file/pom.xml clean package
-Dmaven.test.failure.ignore=true` (red at the python3 exec step on this machine, as master, after compile and surefire);
`bash differential.sh <head classes> <out> 200 1414677829 <jdk-21>` (DIFFERENTIAL PASS, sha256 9a1292f7ecc2);
`python -I mutants.py <root> "<m3-collections jar>;<m3-arrays jar>" <out>/donor-jit.txt <mutants-out> <jdk-21> 200
0x54524545`. Numbers in `GATES.tsv`.
