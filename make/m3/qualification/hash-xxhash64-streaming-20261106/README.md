<!--
SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
-->
# hash-xxhash64-streaming-20261106: the com.synexia xxHash64 streaming and file API into `jdk.internal.m3.hash` (recipe RELOCATE_XXHASH64_STREAMING v4)

One leaf of the M3JDK convergence programme (com.synexia -> M3JDK, ABSORB / MOVE / RETIRE): the streaming xxHash64
state and the file hash of `synexia-fileindex` (`com.synexia.fileindex.content.XXHash64`) join the hash lane of
java.base that `e8a62457e81c` restored (`jdk.internal.m3.hash`, zero-allocation-hashing 0.16 absorbed).

## Decision: ABSORB, with no second kernel

| Question | Evidence | Answer |
|---|---|---|
| Does the owner already stream? | `grep -lE 'update\(|Streaming|FileChannel|hashFile'` over `src/java.base/share/classes/jdk/internal/m3/hash` at master `927e79becaf9` = 0 files; zero-allocation-hashing 0.16 has no streaming xxHash64 | no: ABSORB (not ALREADY) |
| Does the owner have the one-shot? | `M3XxHash.xxHash64` behind `M3LongHashFunction.xx(seed).hashBytes(byte[] / ByteBuffer)` | yes: the donor's `hash(byte[])`, `hash(byte[], long)`, `hash(ByteBuffer)` are NOT relocated; the donor tests call the owner through a declared `oneShot` helper |
| Where does the streaming arithmetic come from? | "JDK owns fast implementations": one xxHash64 kernel | `M3XxHash`'s stripe round, lane merge and tail become package-private atoms (MRL helper extraction, `recipe/owner-patches/M3XxHash.java.diff`); `M3XxHash64Streaming` composes the same atoms; the donor's copy of the arithmetic is REPLACEd, never copied |

`M3XxHash64Streaming` (top-level, as the lane keeps public shapes top-level; M3XXH3 nests only its facade subclasses):
`new M3XxHash64Streaming(seed)`, `reset()`, `update(ByteBuffer)` (consumes the buffer's remaining bytes, any byte
order, heap / read-only / direct; full stripes go through the instance's 32-byte scratch block, no allocation per
stripe: the point of recipe crate `m3-xxhash64-block-reuse-20261031`), `getValue()` (repeatable, does not modify the
state), `hashFile(Path)` / `hashFile(Path, long)` (256 KiB direct-buffer `FileChannel` reads), `DEFAULT_SEED`
(`0x9E3779B97F4A7C15L`, the donor's no-seed default). For every input, `getValue()` equals
`M3LongHashFunction.xx(seed).hashBytes(bytes)`.

## Donor and lineage (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/com.synexia` develop `ca1aafc4d94f9dd1ee288226dcca070cc60991c9` (2026-11-06 16:35 +0530), pinned once (the shared clone's FETCH_HEAD moves) |
| owner blob | `e7a5791a4333806e8ebf28a9167172c55fdb59a5` = the POST-image of crate `m3-xxhash64-block-reuse-20261031` (PR #14848; `hash-pinned-java/.../XXHash64.java.txt`), live only under `m3/qualification/master-landing-20261028/retained-variants/pr14848/synexia-fileindex/...`, next to the crate's executed evidence (13 tests PASS on Temurin 21.0.13, 12,584,832 -> 1,920 allocated bytes per 8 MiB, `javap` API identical) |
| superseded | `aacddfa8b7dca10d4a3ab5f658213046939f7234` = the live develop `synexia-fileindex/.../XXHash64.java` = the crate's PRE-image (`XXHash64.java.before.txt`): `new byte[32]` per stripe; one hunk apart (lines 288-290); OMIT row |
| tests | `XXHash64StreamingTest` `e813c1ae9577`, `XXHash64StandaloneProof` `a5303cc84a1b`, `XXHash64AllocationProbe` `2b3d933711d9` (identical on the develop path and under retained-variants/pr14848) |
| held | `third_party/lz4-java/.../StreamingXXHash64.java` `db1e88b4dcc5` and `XXHash64.java` `32572072b95a`: a second streaming family in the donor tree, NEC (not decided, nothing copied) |
| recipe | `recipe/relocate.py` v4: `python -I m3/qualification/hash-xxhash64-streaming-20261106/recipe/relocate.py --donor-git <clone> --donor-rev ca1aafc4d94f [--check]` |
| map | `recipe/RELOCATION_MAP.tsv`: 4 FILE, 1 PACKAGE, 1 EXTERNAL, 4 TYPE, 3 UNNAMED, 1 NESTED, 3 LIFT, 4 REPLACE, 2 DROP (Lombok), 8 TOKEN, 5 INSERT, 1 OWNER, 4 OMIT, 2 NEC |
| provenance | `recipe/PROVENANCE.tsv`: donor commit, blob, donor and target sha256 per FILE row (`body_match_renamed` = SPLIT for the owner, TRUE for the tests), OWNER pre/post sha256 |

relocate.py v4 is bitmatrix v3 (PR #2521) minus the C/JNI path, plus: SPLIT (`NESTED` + `LIFT`: the nested
`StreamingHash64` becomes the target type, the `DEFAULT_SEED` / `CHUNK_SIZE` constants and both `hashFile` methods are
lifted in by sha256 pins), `REPLACE` (four donor blocks pinned by sha256 give way to `recipe/blocks/*.java.txt`, which
call `M3XxHash.lane1..4`, `round`, `mergeLanes`, `shortStart`, `tail`), `TOKEN` (file-scoped whole-token renames: the
stripe size `32` -> `M3XxHash.STRIPE_BYTES`, the test calls onto the moved and owner APIs), `UNNAMED` (jtreg default
package), `INSERT` (jtreg tags, the `oneShot` helper) and `OWNER` (a pinned unified diff of an existing owner file:
pre `6c9d4f2a8775` -> post `40ab4c85f956`, `git apply --check` first, refused on drift). BRACE_INDENT re-indents the
donor's column-0 closing braces (whitespace only). Idempotent: `--check` reports `changed=0`.

## Gates (2026-11-06, JDK 21.0.9; see GATES.tsv)

`JAVA_HOME=<JDK 21> bash m3/qualification/hash-xxhash64-streaming-20261106/recipe/verify.sh <donor clone> ca1aafc4d94f9dd1ee288226dcca070cc60991c9 <scratch> <maven repo> 10000 <base>`

| Gate | Result |
|---|---|
| recipe | `files=4 owners=1 held=6 changed=0 provenance=UNCHANGED` |
| patch_compile | the 17 hash sources with java.base's flags (`-g -Xlint:all -Werror -Xlint:-this-escape -XDstringConcat=inline --patch-module java.base=...`): 0 warnings, 43 classes |
| contract | donor `XXHash64StreamingTest` on the donor: 3/3; relocated `M3XxHash64StreamingTest` on the patched java.base: 3/3 |
| proof | donor and relocated proof mains print the same `PASS: 17 lengths x 4 seeds, streamed and file parity, zero-seed vector` |
| dump | `M3XxHash64StreamingDump` over the donor (stock JDK, unchanged donor bytes + Lombok/SLF4J) and the relocated surface (patched java.base): byte-identical, 0 MISMATCH lines; see GATES.tsv for the sha256, counts and corpus |
| mutant | the relocated `getValue` with its odd tail byte dropped: refused (MISMATCH lines, exit 1): the dump is live |
| lane_tests | `M3HashAtomsTest` + `M3HashJitAtomsTest` on the patched java.base, prebuilt vendored oracle and `-Dtest.src`: 35/35 both (the lane's recorded 16 + 19) |
| naming | `jdk_naming.py` over the leaf's three paths: `moves=0 rewrites=0 refusals=0` (plan and apply) |
| scope | only `src/java.base/share/classes/jdk/internal/m3/hash/**`, `test/jdk/jdk/internal/m3/hash/**`, this directory |
| probe | allocation figures (30 MiB, 120 x 256 KiB direct updates), never a verdict: GATES.tsv |

Lessons of the run: `RECIPE_MEMORY.tsv`. Attribution: `NOTICE.md`.

## Open items (not in this PR)

- The donor side of com.synexia keeps `XXHash64` (PRE-image on the develop path); RETIRE there means pointing its
  callers at the M3JDK owner (`M3XxHash64Streaming`, `M3LongHashFunction.xx`) in a com.synexia PR; PR #14848's
  POST-image still has to reach the develop main path if the donor keeps its copy meanwhile.
- `jdk.internal.m3.hash` is not exported: library callers outside java.base reach the streaming API only through
  `--add-exports` (as the lane's tests do) until a facade module exports it.
- The lz4-java streaming xxHash64 family in the donor tree (NEC rows) is a second candidate owner shape; not decided.
- jtreg itself was not run (no jtreg on the host); the two jtreg tests ran through the JUnit console and `java` with
  the jtreg `@modules` flags.

## Files

| File | Purpose |
|---|---|
| `src/java.base/share/classes/jdk/internal/m3/hash/M3XxHash64Streaming.java` | the relocated streaming state + file hash, composing the owner's atoms |
| `src/java.base/share/classes/jdk/internal/m3/hash/M3XxHash.java` | OWNER patch: package-private kernel atoms, behaviour preserved |
| `test/jdk/jdk/internal/m3/hash/M3XxHash64StreamingTest.java`, `M3XxHash64StreamingProof.java` | the relocated donor contract (jtreg `@run junit`) and proof (`@run main`) |
| `test/jdk/jdk/internal/m3/hash/M3XxHash64StreamingAllocationProbe.java` | the relocated allocation diagnostic (no `@test`) |
| `test/jdk/jdk/internal/m3/hash/M3XxHash64StreamingDump.java` | the differential dump driver (no `@test`) |
| `recipe/relocate.py`, `recipe/RELOCATION_MAP.tsv`, `recipe/PROVENANCE.tsv`, `recipe/blocks/`, `recipe/owner-patches/` | the recipe, its map, pins, declared blocks and the owner patch |
| `recipe/differential.sh`, `recipe/verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv`, `NOTICE.md` | gates, their record, lessons, attribution |
