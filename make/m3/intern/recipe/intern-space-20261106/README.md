<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# m3/intern: owner-scoped primitive intern space (leaf intern-space-20261106)

Module `org.m3jdk.intern` (package `jdk.m3.intern`), Maven coordinates `com.m3:m3-intern:0.1.0-SNAPSHOT`, Java 21,
`java.base` only; a default module of `make/m3/pom.xml` after `collections`. Leaf of the M3JDK convergence programme
(plan row text-hash rank 8, `intern-space-family`, decision ABSORB).

## What it is

| Type | Contract |
|---|---|
| `M3InternSpace` | four append-only stores of one owner: `atoms()` / `blobs()` (`ByteStore`: collision-verified byte entries over `ByteArena`, 64-KiB pages), `shapes()` (`TupleStore`: kind-separated long tuples over `LongArena`, 8,192-long pages), `graphs()` (`GraphStore`: strict DAG nodes, every child an existing local node); equality candidates are checked page by page with `Arrays.mismatch`; `internString` (exact UTF-16 units), `internType` (loader scope + descriptor + type arguments), `internPath` (lexical), `internFrozenInts`; `freeze`; deterministic insertion-order archive `writeTo` / `readFrom` (magic `0x4d334953` M3IS, version 1, bounded untrusted reads, strict replay); package-private `TupleStore.scopedMember` (allocation-free scalar read for the trie) |
| `M3InternShapes` | typed projections over one space: loader-scoped method signatures, ordered record layouts, frozen `long[]`, canonical `BitSet` masks, lexicon-scoped token tuples |
| `M3InternPathTrie` | prefix-sharing lexical paths as (root, parent, atom, depth) tuples of the shape store; `intern`, `append`, `findExisting` (frozen space), `depth`, `parent`, `lastSegment`, `segments`, `isPrefix`, `commonPrefixLength` |
| `M3InternArchiveGuard` | bounded SHA-256 container of the archive (magic `0x4d334947` M3IG, version 1, 44-byte header): byte and record budgets, allocation-free structural preflight (arity, forward DAG edges, trailing bytes) before the original reader |

Handles are meaningful only inside their originating space; no Java object, class, class loader or native pointer
is interned.

## Owner check (before any port)

`git grep -n -i intern` over `m3/` and `src/java.base/share/classes/jdk/internal/m3` (target, qualification and
vendor trees excluded), restricted to class / record / interface / enum declarations, at master `e5f826ace24c`
(after PR #2532 moved 13 owners from `m3/ports/collections` into `m3/collections`) and again at `cf0429ac1d49`
(after PR #2541 absorbed primitives wave 6 into `jdk.m3.collections`), widened to `*Arena*` types and to
`java.lang.M3StringPool`:

| Type found | Contract | Verdict for the donor family |
|---|---|---|
| `jdk.m3.collections.M3StrongValuePool<T>`, `M3WeakValueCanonicalizer<T>`, `M3ImmutableId` (PR #2516) | canonical pools of Java objects (equals / hashCode, WeakHashMap), packed object ids | different contract: object canonicalization, no primitive content arena, no archive |
| `jdk.m3.lexicon.M3OverflowInterner` (PR #2525) | (language id, String) -> overflow id in a process table | different contract |
| `java.lang.M3StringPool` (java.base, package-private, blob `d17f97b785ea`) | process-wide canonical owner pool of the M3String plane (UTF-16 units, tuple DAG of child coordinates, mapped lexicon images) | different contract: one VM-wide string plane, no owner-scoped handles, no portable archive; the donor states it is not transparent JDK object interning |
| `jdk.m3.collections.M3LongArena28` (blob `6a0e0e872bbd`) | append-only long arena, int offsets in 4,096-value blocks (2^28 slots), `append` / `appendAll` | an arena without interning: no hashing, no collision match; see the NEC row below for the donor's private `LongArena` |
| `M3RopeArena`, `LocalM3Arena`, `M3SharedArenaVerifier` | rope node arena, explicit allocator ("NOT an intern cache"), arena snapshot verifier | different contracts |
| `M3UiInterningBench`, `M3InternedLanguageDump`, `M3InternedLanguageSubstrateTest`, `MIndexJoinedStorageIntern` (template text) | benches, test dumps, a template | not owners |
| the 13 owners moved by PR #2532 and the wave-6 owners of PR #2541 (`M3DenseHash`, `M3PrimitiveArrayList`, `M3PrimitiveHashSet`, `M3PrimitiveSortedTable`, ...) | primitive collections | no byte/long content interning, no archive |
| `java.base` `TokenInterner` | absent at both masters | - |

No M3JDK class implements byte/long arena interning with page-bounded matches and an archive, so no donor class is
ALREADY: all four are **ABSORB** (`M3InternSpace`, `M3InternShapes`, `M3InternPathTrie`, `M3InternArchiveGuard`).
`FOSS_REUSE_DECISION.tsv` records the NO_FIT rows.

NEC (a human decides equivalence, nothing overwritten):

| Donor | M3JDK owner | Note |
|---|---|---|
| `M3InternSpace.LongArena` (private nested, blob `0c06c4dfab78`) | `jdk.m3.collections.M3LongArena28` (blob `6a0e0e872bbd`) | both append long cells into lazily allocated pages; the donor addresses long offsets in 8,192-long pages up to the int page limit and adds page-bounded `matches` and a scalar `get` for collision checks, the owner addresses int offsets in 4,096-value blocks capped at 2^28 slots with no match; backing the tuple store by the owner would change the address bound and add a `jdk.m3.collections` dependency, so the private arena is relocated as is |

## Module decision

A new module `m3/intern`, not `m3/collections`: the canonical pools live in `m3/collections`, but the family is not
pool-shaped. It is an owner-scoped primitive handle space with its own wire format, digest container and path trie,
it explicitly refuses Java object interning, needs nothing from `jdk.m3.collections`, and its package-private seam
(`TupleStore.scopedMember`, used by the trie) wants a package of its own rather than the crowded collections package
that session com-synexia-63 is consolidating right now. The module adds one `<module>intern</module>` line to
`make/m3/pom.xml`.

## Donor, lineage and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/com.synexia` (first-party, private) develop `fd9a233680b43197eb8c5745a729c342ca56c4e2` (2026-11-06) |
| donor files | `synexia-common` intern package: 4 main classes, 7 proof mains, 7 JUnit wrappers (blob and sha256 of each in `PROVENANCE.tsv`; main blobs `0c06c4dfab78` space, `af98b1c63deb` shapes, `a1a834294f37` trie, `9bb73f2f1d70` guard) |
| donor history | 8 feature commits 2026-10-31 .. 2026-11-01 (`e1ebe0048cf` foundation, `d6c3bfb4bda` shapes, `b82397680e9` archive guard, `b387875b2c2` snapshot fix, `f11d589219d` chunk match, `04cb4055068` path trie, `f9c1d9b2e61` + `78ea9b1ab7d` scalar reads); the last `--full-history` touch of all four files is the superset fold `5052329e6b2` (2026-11-06), which kept the blobs |
| recipe | `relocate.py` v4 (`RELOCATE_INTERN_SPACE`, derived from `m3/arrays/recipe/bitmatrix-20261106/relocate.py` v3): `python -I relocate.py --donor-git <clone> --donor-rev fd9a233680b4... [--check]` |
| map | `RELOCATION_MAP.tsv`: 18 `FILE`, 1 `PACKAGE` (donor package -> `jdk.m3.intern`), 18 `TYPE` (identity), 1 `EXTERNAL` (JUnit 5), 2 `LITERAL` (the chunk-match proof's `Class.forName` names of the private arenas), 4 `CTOR`, 7 `PIN`, 9 `OMIT` |
| provenance | `PROVENANCE.tsv`: donor commit, blob, sha256, target sha256, `body_match`, `body_match_renamed`, `declared_drops`, `declared_ctors` per file |
| naming | `python -I make/m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/intern/` (files staged first: the tool reads `git ls-files`): 0 moves, 0 rewrites, 0 refusals |

Lineage (sha256 prefixes; the donor's hash-pinned stage manifests `m3-intern-*-2026103x/manifest.tsv`):

| File | Stages | Live develop blob |
|---|---|---|
| `M3InternSpace` | ABSENT -> `01baa19b` space -> `b68b07fd` snapshot -> `b6068a15` chunk-byte -> `dfad2076` chunk-long -> `c872be0f` trie-scalar-core (trie-scalar-path seals it unchanged) | `c872be0f` = last postimage |
| `M3InternPathTrie` | ABSENT -> `7b262551` path-trie -> `91dc3228` trie-scalar-path | `91dc3228` = last postimage |
| `M3InternShapes`, `M3InternArchiveGuard` | ABSENT -> `c71b6339` / `4b0266e1` | equal |
| 7 proofs, 7 wrappers | each ABSENT -> its stage postimage | equal |

The chunk-byte and chunk-long postimages differ from the live blob only because later stages refined it (lineage, not
drift). The two retained variants `m3/qualification/master-landing-20261028/retained-variants/pr15783/` and
`pr15906/` of `M3InternSpace` hold blob `59689ab1630d` = the first-stage postimage `01baa19b` (no snapshot clone, no
page-bounded match, no scalar seam): an older lineage. The develop head is the newest complete lineage; the donor's
three crates (`m3-intern-chunk-match-20261031`, `m3-intern-shapes-20261031`, `m3-intern-trie-scalar-20261101`) state the
figures reproduced below.

The recipe rewrites only the license header (the donor's block-comment SPDX header becomes the M3JDK header), the
`package` line, mapped `import` lines, the declared literals, inserts the declared constructors, and one
`//FLLDCIM: RELOCATE` anchor (donor blob URL at the donor commit) before each top-level type. `body_match=TRUE` for
16 files; `M3InternSpace` (4 `CTOR` rows) and `M3InternChunkMatchProof` (2 `LITERAL` rows) are `body_match=FALSE`,
`body_match_renamed=TRUE`: read back from disk, every other byte equals the donor's. Idempotent (`--check`
`changed=0` after the run and after `jdk_naming.py`); refuses an unmapped package or import, an absent or ambiguous
`CTOR` class, a `LITERAL` that matches nothing, and a `PIN` whose count changes.

Declared rows, each with its reason in the map:

- `CTOR` x4 (`M3InternSpace`, `ByteStore`, `TupleStore`, `GraphStore`): public classes without a declared constructor
  in the exported package; `-Xlint:all` reports `missing-explicit-ctor` (4 warnings on the donor bytes, invisible in
  the donor's classpath build) and `-Werror` refuses. The inserted `public X() { }` has the implicit constructor's
  access and body (javap gate), so the module keeps `-Xlint:all -Werror` with no exclusion.
- `LITERAL` x2: `Class.forName("<donor package>.M3InternSpace$ByteArena" / "$LongArena")`; `jdk_naming.py` rule `PKG`
  would rename them to `jdk.internal.m3.common.intern...` and the proof would fail.
- `PIN` x7: `0x4d334953; // M3IS` (space and guard), `VERSION = 1;`, `0x4d335459; // M3TY`, `0x4d334947; // M3IG`,
  `0x4d335052; // M3PR`, `0x4d33504e; // M3PN`: wire-format magics, version and archived tuple kinds are never renamed.
- `OMIT` x9: the donor's `synexia-openrewrite-recipes` `M3Intern*RecipeTest` classes (tests of its hash-pinned
  snapshot engine per stage, not of the family); blobs pinned in the run log.

## Hard-limit notes (recorded, not fixed)

Behaviour-preserving relocation; the M3-SCALE hard limits are not applied to donor bodies here:

| File | Lines | Methods > 15 lines (longest) | `for` / `while` loops |
|---|---|---|---|
| `M3InternSpace` | 535 (> 300 class limit) | 6 (26) | 27 |
| `M3InternArchiveGuard` | 174 | 2 (40) | 3 |
| `M3InternPathTrie` | 196 | 2 (20) | 7 |
| `M3InternShapes` | 169 | 1 (16) | 12 |

No `System.out` and no `null` return in main code (the proof mains print their figures).

## Build and gates

| Step | Command |
|---|---|
| compile, test, jar | `JAVA_HOME=<JDK 21> mvn -B -ntp -f m3/intern/pom.xml package` (`-Xlint:all -Werror`, `proc none`; surefire 3.5.4 runs the 7 donor JUnit wrappers, each calling its proof's `run()`) |
| from the control reactor | `mvn -f make/m3/pom.xml -pl intern package` |
| proofs | `java -cp target/classes;target/test-classes jdk.m3.intern.M3Intern<Name>Proof` (prints `PASS checks=N`) |
| differential | `bash recipe/intern-space-20261106/differential.sh <donor clone> <rev> <scratch> [operations]` after `package` |
| all gates | `bash recipe/intern-space-20261106/verify.sh <donor clone> <rev> <scratch> [maven repo] [base rev]` |

`M3InternSpaceDump` (test scope, M3JDK-authored) replays one seeded workload (`SEED = 0x2026_1106_494E_544E`)
through the public API: 100,000 byte chunks and 100,000 long tuples with duplicates, near-duplicates and the boundary
lengths 0, 1, page - 1, page, page + 1 and three pages (chunks straddle page boundaries inside the contiguous arena;
each boundary chunk is re-interned and re-interned with its last cell flipped), 20,000 graph nodes with edges and
refused children, 2,000 typed-shape rounds, 100,000 path-trie inserts with shared prefixes (special segments: empty,
`/`, `.`, `..`, unpaired surrogate, case variants), 100,000 prefix / common-prefix lookups, 10,000 `findExisting`
lookups (half absent), the core archive (sha256, read-back, re-archive) and the guard container of the whole space
plus 23 verdict lines on a small space (2 accepted, 21 refused or rejected) (unfrozen, bad magic, wrong version, truncation, digest mismatch, inner header,
trailing bytes, record and byte budgets, invalid arguments, forward DAG edge, duplicate entry). differential.sh
compiles it a second time with its package line set to the donor package and requires byte-identical dumps.

GATES.tsv holds the figures of the 2026-11-06 run.

Consumers depend on the module with

```xml
<dependency>
  <groupId>com.m3</groupId>
  <artifactId>m3-intern</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```
