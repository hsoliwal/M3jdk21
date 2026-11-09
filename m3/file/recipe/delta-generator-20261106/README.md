<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# delta-generator-20261106: relocate the chunk-sequence delta generator into `jdk.m3.file` (recipe RELOCATE_CHUNK_DELTA v3)

Leaf `fileindex-delta-generator` (rank 7, decision ABSORB) of the FS/PMEM convergence plan
(`C:/m3-superset/conv/plans/fs-pmem.tsv`). Two production classes join the existing module `jdk.m3.file`
(`m3/file`, package `jdk.m3.file`, already exported; `pom.xml` and `module-info.java` unchanged):

- `M3ChunkDeltaGenerator`: `generate(oldRoot, oldChunks, newRoot, newChunks)` is the longest-common-subsequence
  edit script over two ordered sequences of chunk hashes with the donor's deterministic insertion-on-tie backtracking
  (one direction bit per LCS cell plus one `int[m+1]` row: 5,000 x 5,000 chunks fit a 48 MiB heap, where the
  preimage's `int[n+1][m+1]` matrices overflow it); `editDistance` is the substitution-aware Levenshtein distance on
  a rolling row of `min(n,m)+1` ints adapted from Apache Commons Text `LevenshteinDistance` (Apache-2.0);
  `generateWithSizes` sizes the INSERT operations from a `Map<String, byte[]>` of chunk bytes; `areIdentical`.
- `M3ChunkDelta`: the delta (`oldRoot`, `newRoot`, sealed `Operation` = `Keep` | `Insert(hash, size)` | `Delete`),
  `apply(old)` -> new sequence, `isEmpty`, `getStats()` (kept / inserted / deleted / insertedBytes / changeRatio).

A chunk hash is the lowercase hex `String` already used by `M3FileDelta.Entry.sha256`; the generator compares hashes
only with `equals()`. Complexity statements only; no throughput claim (the donor README makes none either).

## Owner decision and overlap (NEC)

`m3/file` is the owner: its module already holds the content-identity and delta family (`M3FileDelta`,
`M3FileDeltaLedger`, `M3File.fingerprint/contentDelta`) and the hex-digest representation the generator needs;
`m3/indexdb` holds SimHash fingerprints and the semantic index, no SHA-256 sequences. The overlap check found no
second owner of the donor's function; every neighbour is recorded as a NEC row in [GATES.tsv](GATES.tsv) (no
equivalence decided):

| neighbour | what it does | why it does not cover the donor |
|---|---|---|
| `jdk.m3.file.M3FileDelta.compare` | two-pointer merge of two snapshots sorted by (kind, path): ADDED / REMOVED / MODIFIED per path | keyed by path, not by position; no alignment of an ordered sequence, no edit distance |
| `jdk.m3.file.M3FileDeltaLedger` | per-slot CREATED / MODIFIED / REMOVED bits over a scanned directory | same: per path, not per sequence position |
| `jdk.m3.file.M3File.contentDelta` | ADDED / REMOVED / CONTENT_CHANGED between two fingerprints of one file | one file, three bits |
| `jdk.m3.collections.M3ListDiff.diff(long[], long[])` | keyed-snapshot diff by longest increasing subsequence of new positions, moves reported, keys must be unique | chunk hash sequences repeat hashes (the proof's binary alphabet, `Collections.nCopies`); the LIS/patience policy is not the donor's LCS insertion-on-tie script; no distance |
| `jdk.m3.algorithm.M3DynamicProgrammingAlgorithms.longestCommonSubsequence / editDistance(CharSequence, CharSequence, M3ProgressMonitor)` | LCS length and edit distance over characters with a progress monitor | scalar results over `char`s, no edit script, no hash sequences; a later leaf may unify the two rolling-row distances, not this one |
| `jdk.m3.file.M3Hash256` (module `org.m3jdk.file`, `m3/ports/file`) | 32-byte SHA-256 value type | lives in the other module that exports the split package `jdk.m3.file`; this module cannot require it, and adds no third hash type (the plan row named it as the mapping target; the split package is the plan's hazard 12) |

## Donor and lineage

| item | value |
|---|---|
| donor | `hsoliwal/com.synexia` develop `c278513bd458f4c9a69e4f5fe261c9450368be3b` (2026-11-06; the scouted `ca1aafc4d94` is an ancestor with the same blobs) |
| owner postimage | `synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-fileindex-cache-delta-20261031/DeltaGenerator.java.txt`, blob `5fb5a5eda7b3`, sha256 `b23fc231...` (the reviewed template of the donor crate `m3-fileindex-cache-delta-20261031`, landed `4a2e6452a7a` 2026-10-31; byte-identical to `m3/qualification/master-landing-20261028/retained-variants/pr14870/synexia-fileindex/.../DeltaGenerator.java`) |
| develop main path | `synexia-fileindex/src/main/java/com/synexia/fileindex/content/chunk/DeltaGenerator.java`, blob `1a4fec6e7d5c`, sha256 `4d491274...`: the **preimage** (quadratic `int[][]` matrices). The recipe landed as a template but was not applied on develop's main path, while the develop contract test (`DeltaGeneratorContractTest`, blob `20cce0f1c477`) is the postimage's test (its `-Xmx48m` probes fail on the preimage). The postimage is therefore the newest complete lineage and is ported; the preimage is a third lane of `differential.sh`. |
| delta type | `synexia-fileindex/src/main/java/com/synexia/fileindex/content/chunk/ChunkDelta.java`, blob `ed7af3f28af5` |
| proof | `DeltaGeneratorContractTest.java` (blob `20cce0f1c477`) and `DeltaWorkspaceProbe.java` (blob `4f21f47b00c9`) |
| third party | Apache Commons Text `LevenshteinDistance` at `fcf5f4838bd59a303043c1b5e3b0b48b36452664`, Apache-2.0: the two attribution comment lines stay in `M3ChunkDeltaGenerator`, the module [NOTICE](../../NOTICE) carries the upstream NOTICE text, and `donor/` keeps the upstream LICENSE and NOTICE plus the donor recipe's README and NOTICE verbatim |
| not ported | `SHA256`, `Chunk`, `ChunkStore` (Synexia owners of the content store; `DONOR_DEP` rows compiled on the donor side of the differential only), `FastChangeDetector` (RETIRE-copy per the plan) |

## Relocation (recipe, never by hand)

`python -I m3/file/recipe/delta-generator-20261106/relocate.py --donor-git <clone> --donor-rev c278513bd458f4c9a69e4f5fe261c9450368be3b [--check]`

[RELOCATION_MAP.tsv](RELOCATION_MAP.tsv): 4 `FILE` rows, 4 `COPY` rows (verbatim notices), `PACKAGE
com.synexia.fileindex.content.chunk -> jdk.m3.file`, `TYPE` rows `DeltaGenerator -> M3ChunkDeltaGenerator`,
`ChunkDelta -> M3ChunkDelta`, `SHA256 -> String`, `ChunkStore -> Map<String, byte[]>`, the two test names, 8 `DROP` rows
(the `SHA256` import that maps onto `java.lang.String`; lombok's `@Slf4j` import and annotation, a logger no donor line
references), 3 `IMPORT` rows (`java.util.Map`, `java.util.HashMap`), 1 `AFTER` row (the explicit public no-argument
constructor the module's `-Xlint:all` `missing-explicit-ctor` lint requires of a class in an exported package; the
donor relied on the default constructor), 14 `SUBST` rows (five `toShortHex()` display sites -> `substring(0, 12)`, its
definition on a hex; the tests' hash constants and store constructor), 2 `EXTERNAL` rows (JUnit 5), 4 `DONOR_DEP` rows.

The recipe rewrites only the license header (the donor's Apache block comment -> the M3JDK header), the package line,
mapped imports, the declared drops, imports, insertion and substitutions, the mapped names as whole-word tokens, and
one `//FLLDCIM: RELOCATE` anchor before each top-level type naming the donor blob URL at the donor commit. Everything
else is byte for byte: [PROVENANCE.tsv](PROVENANCE.tsv) records `body_match_renamed=TRUE` for the four relocated
files (the donor body with the declared rows applied equals the relocated body minus the anchor), the counts of
substitutions / drops / imports / inserts per file and `changed_lines` (every differing line of the whole file,
header and anchor included: 125 / 61 / 88 / 20). Output is LF (what the repository stores under `core.autocrlf`);
`--check` compares LF-normalized bytes, so a CRLF checkout is a fixed point. Derived from
`m3/indexdb/recipe/hamming-topk-20261106/relocate.py` v2 (DROP, IMPORT, AFTER, COPY rows, the block-comment header,
SUBST on FILE targets and LF handling added; MUTANT rows not carried).

## Proofs

- `src/test/java/jdk/m3/file/M3ChunkDeltaGeneratorContractTest.java` (JUnit 5, surefire test phase): the donor's
  proof as it is: 127 x 127 = 16,129 exhaustive binary sequence pairs and 300 seeded sequential-list pairs, each
  checked against the frozen quadratic oracle for the exact script (insertion-on-tie policy), the exact distance and
  `apply()` reconstruction; substitution distance distinct from the script; `NegativeArraySizeException` on
  `Integer.MAX_VALUE` dimensions; null-hash and null-list semantics; identical roots and `generateWithSizes`; two
  subprocess probes `-Xmx48m` (`script`, `distance`) over 5,000 x 5,000 chunks.
- `src/test/java/jdk/m3/file/M3ChunkDeltaWorkspaceProbe.java`: the bounded probe, also run by `verify.py` on the host
  and on the linked image (`bounded_probe_contracts`, markers `PASS script 5000x5000` / `PASS distance 5000x5000`),
  which registers both new test sources in the module's contract inventory.
- [differential.sh](differential.sh): the donor postimage, the develop preimage and the relocated owner compiled and
  driven by [differential/M3ChunkDeltaDump.java](differential/M3ChunkDeltaDump.java) (reflection, compiled into
  scratch) over the proof's 16,129 + 300 pairs, 100,000 seeded file-row pairs, 1,000 equal-root pairs, 1,000
  `generateWithSizes` cases over a chunk store, the proof's exception cases and the 5,000 x 5,000 probe: every script,
  distance, stats, `apply()` reconstruction and exception class printed with hashes as hex; the three dumps must be
  byte-identical; the three 48 MiB probes (relocated and donor PASS, preimage `OutOfMemoryError` as the donor README
  states); `javap -public` of the owner and the delta type with its six nested types, names mapped, identical.

## Gates (2026-11-06, see GATES.tsv)

Figures and digests of every executed gate are in [GATES.tsv](GATES.tsv); corrections in
[RECIPE_MEMORY.tsv](RECIPE_MEMORY.tsv). [verify.sh](verify.sh) replays recipe fixed point, naming fixed point, the
module build, the surefire proof and the differential from the repository root.

Pre-existing on this Windows host and unrelated to the leaf: `M3FileDeltaSelfTest.java:225` expects an LF receipt
header while `PrintStream` writes CRLF, so `mvn -f m3/file/pom.xml package` fails in surefire (`M3FileDeltaTest`) and
in the `verify.py` host lane on the untouched module exactly as with the leaf. The gates therefore record (a) the
baseline and the leaf under `-Dmaven.test.failure.ignore=true` (the leaf's JUnit proof green, the same single
pre-existing failure) and (b) the complete `mvn package`, `verify.py` included (host and linked image), on an
evidence copy whose only change is that assertion made line-separator-neutral (never committed).

## Files

| file | purpose |
|---|---|
| `m3/file/src/main/java/jdk/m3/file/M3ChunkDeltaGenerator.java`, `M3ChunkDelta.java` | the relocated owner and delta type |
| `m3/file/src/test/java/jdk/m3/file/M3ChunkDeltaGeneratorContractTest.java`, `M3ChunkDeltaWorkspaceProbe.java` | the relocated proof and probe |
| `m3/file/verify.py` | one inventory category (`bounded_probe_contracts`), the JUnit proof registered, the probe run on host and image |
| `m3/file/NOTICE` | module notice with the Apache Commons Text attribution |
| `m3/file/README.md` | one section |
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the relocation recipe, its map and its pins |
| `differential.sh`, `differential/M3ChunkDeltaDump.java`, `verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv` | gates and their record |
| `donor/` | verbatim: Apache Commons Text LICENSE and NOTICE, the donor recipe's README and NOTICE |

## Open

- `m3/THIRD_PARTY_NOTICES.md` (outside the module, gate 4) does not yet list Apache Commons Text; the module
  `NOTICE` and `donor/` carry the attribution. One line there is a follow-up.
- A packed-lane overload (hash lanes instead of `List<String>`) and the unification of the two rolling-row distances
  (`M3DynamicProgrammingAlgorithms.editDistance` over characters) are later leaves with their own recipes and oracles.
- The donor's `generateWithSizes` consumer (`ChunkStore`, FastCDC chunking, `ChunkIndex`) stays in Synexia.
