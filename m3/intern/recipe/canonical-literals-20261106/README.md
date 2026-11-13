<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# m3/intern: canonical typed literal pool (leaf canonical-literals-20261106)

`jdk.m3.intern.M3CanonicalLiteralPool` in module `org.m3jdk.intern` (Maven `com.m3:m3-intern:0.1.0-SNAPSHOT`, Java 21,
`java.base` only). Leaf of the M3JDK convergence programme: plan `C:/m3-superset/conv/plans/native-compiler-openjdk.tsv`
rank 8, `canonical-literal-pool`, decision ABSORB, blocked until an interner owner existed. The owner landed with
PR #2549 (merge `feae0743e6c2`, recipe `../intern-space-20261106`): `M3InternSpace`, `M3InternShapes`,
`M3InternPathTrie`, `M3InternArchiveGuard`.

## Scope: the POOL only

This leaf relocates the pool and nothing else. It is never a JVM-wide `ldc` / class-file constant-pool replacement,
never a replacement for `java.lang.String` literal identity, and it rewrites no source literal. The donor's policy
stands as recorded in its crate (`LITERAL_POLICY.tsv`, `DONOR_DECISIONS.tsv`, blobs pinned by the OMIT rows):

| Literal class | Donor status |
|---|---|
| String, floating-point, integral, boolean and char literals | CANDIDATE_ONLY (typed named ids; ldc identity, signed zero / NaN, JLS folding, JNI exactness, UTF-16 exactness to be proven per use) |
| Annotation and switch constant expressions | HOLD (compile-time-constant status must not change) |
| Structural 0/1 and protocol tokens | CATEGORY_REVIEW |
| bulk literal rewrite | DEFERRED (read-only inventory until per-file proof) |

## Dependency mapping (decided first: `DECISION.tsv`)

The donor pool calls one method of one dependency: `M3InternableSpace.constantBytes(kind, bytes)`, which is
`MIndexAtomStore.Builder.internBytes(MIndexAtomDomain.CONSTANT, kind, bytes)`: one atom per (domain CONSTANT, type kind,
payload kind BYTES, exact payload bytes). Each need maps onto the existing public API of `M3InternSpace`:

| Need | Donor | M3JDK owner | Verdict |
|---|---|---|---|
| owner-scoped space (D1) | `M3InternableSpace` | `M3InternSpace` (TYPE row) | MAPPED |
| stable handle per typed literal (D2) | `constantBytes(kind, bytes)` | `space.shapes().intern(CONSTANT, kind, space.blobs().intern(bytes))` | MAPPED |
| separation by domain (D3) | `MIndexAtomDomain.CONSTANT` | tuple kind `CONSTANT = 0x4d33434c` (M3CL), unused by every other kind of the module | MAPPED |
| separation by type (D4) | type lane, kind ids 1-9 | member 0 of the CONSTANT tuple (ids pinned) | MAPPED |
| raw-bit float / double identity (D5) | pool encodes `floatToRawIntBits` / `doubleToRawLongBits` | exact-byte `ByteStore` (hash, length, page-bounded `Arrays.mismatch`) | MAPPED |
| UTF-16 code-unit identity (D6) | pool encodes every unit little-endian | exact-byte `ByteStore` (`blobs()`: `internString` of the atoms store writes big-endian units) | MAPPED |
| payload / domain read-back (D7, test) | `MIndexAtomStore.bytes(id)` / `domain(id)` | `blobs().copy(shapes().members(id)[1])` / `shapes().kind(id)` | MAPPED |
| the store itself (D8) | `MIndexAtomStore` | `M3InternSpace` is the interner | NOT_PORTED (one owner rule) |
| other-domain probe (D9, test) | `symbol(1, "word")` | `internType(1, "word")` (no SYMBOL domain; a TYPE tuple of the same handle space) | MAPPED |
| handle width (D10) | int atom ids | long tuple ids below 2^24, `Math.toIntExact` | MAPPED |

Owner contract where the two differ (recorded, not observable through the donor test or the differential stream):
after `freeze()` the donor builder refuses every admission, a frozen `M3InternSpace` returns the handle of an existing
literal and refuses only a new one (D11); per-entry bound 2^26 bytes (strings up to 33,554,432 units), 2^24 blobs and
2^24 tuples per space (D12).

Alternatives (all NO_FIT): `java.lang.M3StringPool` is the one VM-wide weak M3String plane (no owner scope, no typed
kinds, no int handles, package-private in `java.base`); `jdk.internal.mindex.M3PrimitiveIdentity` is a (kind, raw bits)
key function without a store or handles (not exported by `java.base`; its identity rule agrees with the pool's, kind
numbers differ); `jdk.internal.mindex.M3PrimitiveFixedArena` projects an immutable compile-time image at a fixed
address. None of the three exists in the stock JDK 21 that builds `m3/intern`. Placement (P1): `m3/intern`, next to its
owner; no `java.base` consumer exists, so the plan's `jdk.internal.mindex` placement is not taken and
`m3/compiler/literals` is not needed.

## Donor, lineage and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/com.synexia` (first-party, private), REPO row: develop `b259ec9f5606b9066231874ef06a3d27b4073e89` (2026-11-06) |
| files | `synexia-indexstring` `M3CanonicalLiteralPool.java` blob `08cd5e81412f` (sha256 `1d28d6c9...`), `M3CanonicalLiteralPoolTest.java` blob `094348daebc7` (sha256 `13e77890...`) |
| history | `f1ece21be5f` 2026-10-31 "source-sealed named typed literal pool with per-atom JUnit" (create-only, hash-pinned templates `0001` / `0002` byte-identical to the blobs, manifest ABSENT -> `1d28d6c9` / `13e77890`); superset merge `4c34e35972c` (PR #14977, HOLD); develop through the superset landing #16729 (`137fad41745`) unchanged |
| executable donor | CLOSURE row: `f1ece21be5fb0f7e5413c3f5ed6bdebabbe893bf`. Develop carries the two blobs without `M3InternableSpace` and with a `MIndexAtomDomain` lacking CONSTANT, so the develop pool does not compile; `f1ece21be5f` holds the same blobs (relocate.py refuses otherwise) with `M3InternableSpace` `d260ee4da2c6` and `MIndexAtomStore` `e00ef4f8ff67` |
| recipe | `relocate.py` v5 (`RELOCATE_CANONICAL_LITERALS`, derived from `../intern-space-20261106/relocate.py` v4, itself from `m3/arrays/recipe/bitmatrix-20261106/relocate.py` v3): REPO, CLOSURE, CLOSURE_PATH and REWRITE rows and the `declared_rewrites` provenance column added |
| map | `RELOCATION_MAP.tsv`: 1 REPO, 1 CLOSURE, 13 CLOSURE_PATH, 2 FILE, 1 PACKAGE, 3 TYPE (2 identity, 1 dependency), 1 EXTERNAL (JUnit 5), 8 REWRITE, 9 PIN, 11 OMIT |
| provenance | `PROVENANCE.tsv`: `body_match=FALSE`, `body_match_renamed=TRUE` for both files (`declared_rewrites` 3 and 5): read back from disk, every byte that is not a declared row equals the donor's |
| naming | `python -I make/m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/intern/` (files staged first): 0 moves, 0 rewrites, 0 refusals |

REWRITE rows (the adapter, nothing else):

- pool: `space.constantBytes(` -> `constantBytes(` (9); the `CONSTANT` tuple kind declared after `TRUE_VALUE`; the private
  adapter `constantBytes(kind, bytes)` inserted before `littleEndianInt`.
- test: `M3InternableSpace.create()` -> `new M3InternSpace()` (10); `space.freeze().bytes(` -> `payload(space, ` (7);
  `space.symbol(1, "word")` -> `space.internType(1, "word")`; `MIndexAtomDomain.CONSTANT, space.freeze().domain(id)` ->
  `M3CanonicalLiteralPool.CONSTANT, domain(space, id)`; the helpers `payload` and `domain` appended.

PIN rows keep the append-only kind ids (`STRING_UTF16(1)` .. `DOUBLE_RAW_BITS(9)`, archived as member 0 of every CONSTANT
tuple) and the test data (the four NaN payloads, the unpaired-surrogate strings, the lone-surrogate char literal). OMIT
rows pin the donor's OpenRewrite receipt test, its three templates and manifest, the recipe YAML, the crate README /
policy TSVs / pom and the crate workflow (snapshot-recipe receipts of the donor, not behaviour of the pool).

## Differential (M3JDK-authored, `M3CanonicalLiteralPoolDump`, test scope)

`differential.sh` compiles the donor pool with its own `MIndexAtomStore` from the CLOSURE tree (javac `-sourcepath` over
the 9 CLOSURE_PATH SOURCE roots, commons-codec 1.22.1, re2j 1.8 and lombok 1.18.42 from the local Maven repository:
1,183 classes, only those the pool reaches), runs the donor test there with the JUnit Platform 1.11.4 console, and
compiles the dump a second time for the donor (package line and seven SIDE lines naming the space API replaced, each
exactly once). The seeded stream (`SEED = 0x2026_1106_434C_4954`, 4,340 structured + 100,000 random admissions) covers
the nine kinds with Long / Integer / Short / Byte extremes, +0.0 / -0.0, infinities, subnormals, 1,000 distinct float and
1,000 distinct double NaN payloads (both signs, quiet and signaling; re-admitted shuffled), chars with lone surrogates,
strings with unpaired surrogates, supplementary code points and NULs (always fresh `String` objects), null strings and a
null space, same-value-different-type groups (equal payload bytes under different kinds among them) and
same-value-different-domain groups (TYPE, PATH and int-sequence entries, scopes 0 and 1). The dump prints the
equivalence classes of the handles named by first admission index (never a handle value), so identical dumps mean an
identical handle-equality relation; it also checks that relation against reference keys (`Float.floatToRawIntBits`,
`Double.doubleToRawLongBits`, the String's UTF-16 code units, kind and scope) and every payload read back from the space
against the same references. A mutation that collapses the literal kind in the adapter fails it (RELATION
violations=3165, CROSS bytes merged=14/14).

## Hard-limit notes (recorded, not fixed)

| File | Lines | Methods > 15 lines | `for` / `while` loops |
|---|---|---|---|
| `M3CanonicalLiteralPool` (donor body) | 130 | 0 | 4 |
| `M3CanonicalLiteralPoolTest` (donor body) | 141 | 0 | 0 |
| `M3CanonicalLiteralPoolDump` (M3JDK test driver) | 600 (> 300) | 10 (longest 39) | 32 |

The driver follows `M3InternSpaceDump` (443 lines, loops): explicit loops keep the replay order obvious and the -Xint
run short. No `System.out` (the driver prints through `FileDescriptor.out`, LF line ends) and no `null` return.

## Build and gates

| Step | Command |
|---|---|
| relocate | `python -I recipe/canonical-literals-20261106/relocate.py --donor-git <clone> --donor-rev b259ec9f5606... [--check]` |
| compile, test, jar | `JAVA_HOME=<JDK 21> mvn -B -ntp -f m3/intern/pom.xml package` (`-Xlint:all -Werror`; 17 tests) |
| differential | `bash recipe/canonical-literals-20261106/differential.sh <donor clone> <scratch> <maven repo> [random operations]` after `package` |
| all gates | `bash recipe/canonical-literals-20261106/verify.sh <donor clone> <scratch> <maven repo> [base rev]` |

`GATES.tsv` holds the figures of the 2026-11-06 run. Recipe `../intern-space-20261106/differential.sh` now walks only the
main FILE types of its map in its javap gate (this leaf added the first foreign classes to the package; its
`RECIPE_MEMORY.tsv` records the fix); its dump digest `0e6743bb88e2...` is unchanged.

Open items: develop's `M3CanonicalLiteralPool` / `M3CanonicalLiteralPoolTest` do not compile in com.synexia (dangling
`M3InternableSpace`, D0); a public read-back accessor on the pool waits for a consumer (today the tuple layout
(kind, blob handle) under kind `CONSTANT` is the contract).
