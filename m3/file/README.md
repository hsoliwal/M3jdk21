<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3File: narrow NIO file-edge receiver

`com.m3.file.M3File` is a minimal target-owned, Java21/module-info-enabled
read-only access boundary adapted from the canonical Synexia
`synexia-mindex/resource/.../MIndexResourceAccess.java`, Apache-2.0,
Git blob `1b4eb18b102f52be2d1e13589195b47e37f3b090`.

It does **not** create a filesystem implementation or retain a directory
object graph. It preserves the supplied `Path` instance/provider; stat calls
always refresh with NOFOLLOW_LINKS; `readAllBytes(maxBytes)` checks the
visible file type, reads through a caller-closed channel, and enforces both
the observed-size and streamed-length limits. Symlinks and directories do
not become regular files. Path, link and permission behavior remains NIO's.

The broader Synexia `MIndexResourceSnapshot`, `MIndexResourceWorkspace`,
Eclipse markers/deltas, tree scans, permissions and mutation APIs remain
unported. This is not full Eclipse workspace parity, an OS-level file
interning service, or permission to change existing `java.io.File` semantics.

Build `mvn -B -f m3/file/pom.xml test` or the containing Eclipse JMOD
builder in `m3/eclipse/optional-jmod/build.py`.

## Source and class content deltas

`fingerprint(long maxBytes)` admits one bounded raw-byte observation for either
source or binary files through this original file boundary. It retains a byte
count and four SHA-256 words, uses an 8192-byte scratch buffer, and keeps no file
payload. The streamed budget and the regular-file/link rules still apply. It
refuses detected size, modification-time or file-key changes, but does not claim
an atomic filesystem snapshot under concurrent writes.

`contentDelta(previous, current)` compares retained fingerprints without I/O or
parsing. `ADDED=1`, `REMOVED=2`, `CONTENT_CHANGED=4`; zero means matching
fingerprints or two known absences. Null is caller-established absence, never
an unread file or a failed observation. Existing admitted fingerprints should
be reused; calling `fingerprint` again deliberately reads again.

Raw byte fingerprints differ from the parser's UTF-16 and framed source-fact
identities. A matching digest does not certify collision-free equality, class
ABI, compiler bindings or complete task inputs. The existing build planner must
qualify those facts before skipping work. The [shared build contract](../docs/build-delta-invariant.md)
defines source, binary, dependency and configuration invalidation for Maven,
Gradle and the other fork consumers. The [mapping](../docs/name-mapping.d/m3file-build-delta-20261020.json)
keeps this extension with the original product module.

The `test` lifecycle now invokes `verify.py` against Maven's already compiled
module. It compiles the independent contract, checks the named module, builds
a linked image and reruns the same contract there. This includes real javac
compilations with unchanged dependency binaries and a constant-inlining witness.
Run `mvn -B -f m3/file/pom.xml verify` with Java 21 and Python 3.
Alternatively, `python3 m3/file/verify.py --java-home /path/to/jdk21 --output /path/to/proof`
also compiles the original module itself.

## ChangeMap lower bound (recipe replay 2026-11-06)

`M3FileEditBatch.ChangeMap.oldToNew/newToOld` answer in O(log E) through one private
first-equality lower bound instead of an O(E) ordered scan; public API, affinity and
exception behaviour are unchanged. The change is a replay of the com.synexia recipe
`M3FileChangeMapLowerBound20261029` (hsoliwal/com.synexia 4a2e6452a7a) re-sealed onto the
renamed owner in [recipe/changemap-lower-bound-20261106](recipe/changemap-lower-bound-20261106/README.md):
`reseal.py` derives the templates, `apply.py` replays the hash-pinned snapshot, the OpenRewrite
replay test proves the canonical recipe emits the same postimage, and
`src/test/java/jdk/m3/file/M3FileChangeMapBinarySearchProof.java` (also run by `verify.py` on the
host and the linked image) is the ported donor oracle: 50,001 maps, 9,341,740 checks, with a
digest that `differential.py` compares across the receiver, the donor postimage and the preimage.

## Chunk-sequence delta (recipe relocation 2026-11-06)

`M3ChunkDeltaGenerator.generate(oldRoot, oldChunks, newRoot, newChunks)` aligns two ordered
sequences of content-chunk hashes (lowercase hex `String`s, the representation of
`M3FileDelta.Entry.sha256`) into an `M3ChunkDelta` of KEEP / INSERT / DELETE operations: a
longest-common-subsequence edit script with the donor's insertion-on-tie backtracking, one
direction bit per cell plus one `int[m+1]` row (5,000 x 5,000 chunks fit a 48 MiB heap).
`editDistance` is the substitution-aware Levenshtein distance on a rolling row of `min(n,m)+1`
ints, adapted from Apache Commons Text (Apache-2.0; attribution in the file, in [NOTICE](NOTICE)
and under `recipe/delta-generator-20261106/donor/`). `generateWithSizes` sizes the INSERTs from a
`Map<String, byte[]>` of chunk bytes; `M3ChunkDelta.apply(old)` rebuilds the new sequence.
This is the sequence-alignment delta the module lacked: `M3FileDelta` merges a sorted per-path
inventory, `M3FileDeltaLedger` keeps per-slot state bits, `M3File.contentDelta` compares two
fingerprints; none aligns sequences (NEC rows in the recipe's GATES.tsv).

Relocated by recipe from hsoliwal/com.synexia `synexia-fileindex` (the reviewed postimage of the
donor crate `m3-fileindex-cache-delta-20261031`) in
[recipe/delta-generator-20261106](recipe/delta-generator-20261106/README.md): `relocate.py`
(RELOCATE_CHUNK_DELTA v3) over `RELOCATION_MAP.tsv`, pins in `PROVENANCE.tsv`. The donor's proof
`M3ChunkDeltaGeneratorContractTest` (JUnit 5: 16,129 exhaustive binary pairs + 300 seeded pairs
against the frozen quadratic oracle, two `-Xmx48m` subprocess probes) runs in the surefire test
phase; `verify.py` runs the bounded probe `M3ChunkDeltaWorkspaceProbe` on the host and the linked
image; `differential.sh` proves donor postimage, develop preimage and relocated owner byte-identical
over 118,432 pairs (2,983,157 operations).

## Character flags combine (recipe alignment 2026-11-06)

`M3CharacterFlags.combine(left, right)` (package-private, with `ALL_OF`/`ANY_OF`) composes the exact flags of two
adjacent UTF-16 ranges without a rescan: `ASCII`/`LATIN1`/`BLANK`/`EMPTY` hold if both ranges have them, presence bits
if either does, bits outside `ALL` are refused. The class was the donor `IndexCharacterFlags` at its pre-combine blob;
[recipe/character-flags-20261106](recipe/character-flags-20261106/README.md) relocates the donor owner with `combine`
onto it (the #2523 recipe, one `TYPE` row), so it and `jdk.internal.m3.indexstring.M3IndexCharacterFlags` are one body.
Public API unchanged; `differential.sh` proves both classes byte-identical over the exhaustive 1,024 x 1,024 `combine`
table and the #2523 inputs.

## Durable extent store (recipe relocation 2026-11-07)

`M3DurableExtentStore` is a single-writer, append-only store of source-bound byte extents. An `M3ContentExtent`
names a byte range of a source (source id and SHA-256, `[byteStart, byteEnd)`, content SHA-256, logical kind); its
`extentId` is the SHA-256 of the sorted-key canonical JSON of those fields. `write` (a new, immutable key) and
`supersede` (an existing key) verify the exact payload, write and force a content-addressed blob
(`blobs/<sha256>.bin`, published by atomic rename), then write and force a complete, sorted index sealed with its
own SHA-256 and publish it by one atomic rename over `index.m3`. A failure before that rename leaves the previous
index in force (an unreachable blob may remain). `read` refuses a missing, truncated, extended or altered blob
(exact length first, then SHA-256) and returns a fresh array copied out of a READ_ONLY mapping; no mapping escapes.
`writer.lock` admits one writer per directory, across processes.

**Durability is limited to the atomic rename.** The store needs a filesystem with atomic same-directory rename and
fails closed without one. It attempts a directory fsync where the platform allows one and ignores the refusal
(Windows always refuses). There is no portable power-loss guarantee: no Java API orders persistence across a power
loss on every filesystem, and nothing here has been qualified for one. The recipe's crash gate kills a writer
process before every filesystem step of a put (52 kills): every reopen saw the previous or the new index, never a
torn one.

On Windows a blob that was read stays mapped until its buffer is collected; replacing or truncating that file from
outside fails meanwhile. The store itself never rewrites a blob.

Relocated by recipe from hsoliwal/com.synexia `synexia-storage-sdk` (develop 0616288bfa7d, origin 29621ea829f) in
[recipe/durable-extent-store-20261107](recipe/durable-extent-store-20261107/README.md): `relocate.py`
(RELOCATE_DURABLE_EXTENT_STORE v1) over `RELOCATION_MAP.tsv`, pins in `PROVENANCE.tsv`, decisions in
`DECISION.tsv`. The donor's read facade `StorageStore` (segmented MAPPED_FILE and optional PMDK backends) and its
backend-selecting `open(Path, StorageOptions)` are HELD; the default read is restated on `FileChannel`. The schema
ids follow JDK naming (`m3.content-extent.v1`), so extent ids differ from the donor's; index files do not.
`M3ContentExtentTest` and `M3DurableExtentStoreTest` (the donor's) and `M3DurableExtentStoreOracleTest` (the
synexia-mllm extent-store scenarios whose semantics match) run in the surefire test phase; `differential.sh` proves
the donor and relocated dumps byte-identical under JIT and `-Xint` (32,190 lines), runs the crash gate and kills 5
of 5 mutants.

Canonical recipe: `com.synexia.m3jdk.CollectionsContractRefinement20261017`,
dataset `file-content-delta-20261020`, in Synexia's existing
`synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017`.
This is a new sealed dataset of the existing Java/text recipe composition.
Fork planner receivers and full M3JDK21 bootstrap qualification remain separate.
