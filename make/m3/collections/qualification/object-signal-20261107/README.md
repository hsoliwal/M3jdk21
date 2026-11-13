<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# object-signal-20261107: relocate the object first-pass feature-mask signals into `jdk.m3.collections` (recipe RELOCATE_OBJECT_SIGNAL v1)

Rank 4 of the private-repo scout (`S:/m3-scratch/eb-conv/scout-private/PROPOSED_LEAVES.tsv`, `SIGNAL_CENSUS.tsv`),
scope row 9: the **primitive core only** of the object first-pass signal family, ABSORB-gap into `m3/collections`,
next to `M3SignalBooleanQuery`, `M3SignalColumns` and `M3LongBitmapFilterPlan`.

## What it is

`object -> explicit projection -> primitive signal table -> bitwise shortlist -> exact verifier`. The first pass may
admit false positives; it never establishes semantic truth.

| Owner | Role |
|---|---|
| `M3ObjectSignal` | record of one object's numeric projection: kind, domain, logical length, 64-bit exact-hash candidate, **64-bit feature mask**, untouched raw identity, flags `HAS_LENGTH` / `HAS_HASH` / `HAS_IDENTITY` |
| `M3ObjectSignalTable` | the mask table: primitive SoA lanes (byte kind; int domain, length, flags; long hash, identity) and 64 **transposed feature planes** (plane n = bitmap of the rows carrying bit n); `build` under an `M3PoolProgressMonitor`, `signalAt`, `featureBitsAt`, `firstPass` (AND per required bit, AND-NOT per forbidden bit over 64 rows per word, then metadata on survivors only), `primitivePayloadBytes` |
| `M3ObjectSignalQuery` | record query: kind / domain (`ANY_KIND`, `ANY_DOMAIN`), length bounds (`UNBOUNDED`), required / forbidden bits, exact hash and identity under `REQUIRE_HASH` / `REQUIRE_IDENTITY` (each also requires the row's flag); factories `exact`, `maybeContains`, `mask`, `anyMask` |
| `M3ObjectSignalFirstPassResult`, `M3ObjectSignalExactResult` | the counters: total, after the bitwise planes, after the metadata, rejections, survivor ratio; exact checks, exact matches, checks avoided |
| `M3ObjectSignalMatcher` | the exact second stage: the caller's predicate over first-pass survivors only |
| `M3ObjectSignalBitmap` | immutable fixed-size population (all, empty, contains, cardinality, forEachIndex, and, andNot) |
| `M3ObjectSignalMath` | FNV-1a 64 + fmix64 exact-hash candidates of `CharSequence` and `byte[]`; one-word unigram + bigram signatures (no false negative for literal containment); `indexFeatures`; `mix64` |
| `M3ObjectProjector`, `M3ObjectProjectors` | the projector SPI and the kept projectors: `byString` (String hash), `byIndex` (long), `bySignal` (caller mask), `byCanonicalBytes` (byte[]), `custom` |
| `M3ObjectProjectionKind`, `M3ObjectSignalDomains` | enum STRING / INDEX / CANONICAL_BYTES / SIGNAL; reserved domain numbers |
| `M3ObjectSignalOps` | the bulk plane pass: shape checks + the donor's Java kernel |

`java.base` only; the classes join the exported package `jdk.m3.collections`, so `pom.xml` and `module-info.java` are
unchanged. Main 13 files (relocated), test 3 files (the relocated oracle, a module-authored naive reference model and
its JUnit wrapper).

## Donor (and the donor the brief named)

| Item | Value |
|---|---|
| donor | `hsoliwal/synexia-m3scale-apache2` develop `3385d79a2968c770aedf1d3398b2a62f05ebc092` (2026-11-06, the live head), module `m3-object-signal` |
| lineage | `4d99cdd036` 2026-09-23 feat(object-signal): add numeric bitwise first-pass engine (the only content commit) -> `739ac50464` 2026-10-24 WIP sweep (tree emptied) -> `99f808713d` 2026-10-27 exact tree restore -> `3385d79a29` head; the 19 module blobs are identical at the three content states |
| brief's donor | `hsoliwal/com.synexia` develop `fd9a2336` (re-pinned `35474ccf8daf`, blobs unchanged; `git log --follow --full-history`: `da7a4efb35` 2026-09-23 add immutable POJO object signal (Signal 52ad2ae81182, Projection b72c3ccc575f) -> `26f0d3bdbc` 2026-09-23 copy object snapshot SHA lanes once (Signal ee4737e4, Index 4e5ae7ddde0f, Result 345fe981, test 0012eddb92db) -> present with the head blobs at `eb945bb434` 2026-09-28 (Index 650aab08, test 58568cc4) -> merges only to the head) `synexia-indexstring` `MatIndexObjectSignal` ee4737e4b91a, `MatIndexObjectSignalIndex` 650aab083339, `MatIndexObjectSignalResult` 345fe98155ac, `MatIndexObjectProjection` 846e8c207af8, `MatIndexObjectSignalTest` 58568cc44b5e |

The brief's contract (feature masks, kind / domain / length / hash flags, `mask` / `anyMask` / `exact` /
`maybeContains` with `REQUIRE_HASH`, first-pass and exact counters, optional JNI `applyFeatureMasks`) is carried only
by m3scale `m3-object-signal`. The com.synexia `MatIndexObjectSignal` family is another contract: a Hamming first
pass over `structureHash64` + `textSimHash64` + `min(1023, token-count delta)`, a SHA-256 snapshot identity table
with duplicate chains, `rank` by radix sort and one schema / token-space SHA-256 per index; it lives in the search
family's module and depends on `MatIndexStructureKey` / `MatHash256` / the string pool. It is HELD for the search
family's owner with NEC rows (DECISION.tsv D0 / D1).

## Relocation (recipe, never by hand)

```sh
python -I m3/collections/qualification/object-signal-20261107/relocate.py --donor-git <m3scale clone> \
    --donor-rev 3385d79a2968c770aedf1d3398b2a62f05ebc092 [--check] [--donor-subset DIR]
python -I m3/collections/qualification/object-signal-20261107/apply.py [--check]
```

* `relocate.py` v1 (derived from `m3/collections/recipe/immutable-pools-20261106/relocate.py` v2, same donor
  repository and module): header, package, mapped imports, 15 TYPE rows, one `//FLLDCIM: RELOCATE` anchor per file;
  both-mode `DROP_METHOD` / `DROP_LINE` / `REWRITE` rows; new relocate-only `DROP_BLOCK` (count + sha256 pinned) and
  `ADAPT` rows; `SUBSET_ONLY`, `HELD`, `OMIT` rows. `PROVENANCE.tsv`: 14 FILE rows, body_match 14/14.
* Declared changes against the donor: the four search-family / ImmutableId projectors (`DROP_METHOD`); the codec
  parameter of `byCanonicalBytes` becomes a `BiFunction<? super T, ? super M3PoolProgressMonitor, byte[]>` encoder
  (4 `ADAPT` lines; the codec family is a SKIP row of immutable-pools-20261106; an immutable codec fits as
  `codec::encode`); the JNI seam of `NativeSignalOps` (4 `DROP_BLOCK` rows, 31 lines; one `ADAPT` doc line); the
  oracle keeps 4 of its 8 methods and its PASS line loses the JNI flag.
* `apply.py` (`EDITS.tsv`): one row in `m3/collections/JDK_FAMILY_MAP.tsv`.

## Decisions (DECISION.tsv), NEC and HELD

* ABSORB: the primitive core above (D0, D2, D11). ADAPT: `byCanonicalBytes` (D8).
* HELD: the JNI kernel `object_signal_jni.c` (4c3bcedc7980) and its `CMakeLists.txt` (df5f82bbd7a0): a Java-only
  first cut carries the contract (the donor's Java kernel is its reference path, the only one below 256 words or
  without the library), M3 collections keeps JNI opt-in (D7); the `immutableId()` projector (brief scope; its owner
  `M3ImmutableId` exists here); `ImmutableCodec` (codec family); the com.synexia `MatIndexObjectSignal` family (D1).
* SKIP: `indexString()`, `mIndexStringTokens()`, `mIndexStringText()` (search family, session com-synexia-4d); the
  `m3-object-projection` module (D10). OMIT: `ObjectSignalBenchmark`, the donor README and pom.
* NEC (equivalence not decided, both kept): the transposed-plane pass vs `M3StateMaskArrays` / `M3SignalColumns.selectedByState` /
  `M3MaskViewportKernel` / `M3LongAddressKernel` (D4); `M3ObjectSignalBitmap` vs `BitSet` / `M3RoaringBitmap` /
  `M3ConcurrentBitSet` / `M3LongBitmapFilterPlan` (D5); `M3ObjectSignalMath` vs the fmix64 copies,
  `jdk.internal.m3.hash`, `M3LongBloomFilter` (D6); the com.synexia family vs this family and vs `m3/indexdb`
  `M3HammingTopK` / `M3IndexDbFingerprintV1`. NOT_OVERLAP: `M3SignalBooleanQuery` (D3).
* FOSS: FOSS_REUSE_DECISION.tsv (OpenCLAW: no fit; BitFunnel bit-sliced signatures an architectural reference only).

## Oracles and gates

* `M3ObjectSignalSelfTest` (relocated donor oracle, 4 methods): `PASS: 24119 object-signal checks`.
* `M3ObjectSignalReferenceModel` (test scope, module-authored): naive per-row evaluation of the mask and metadata
  laws, the counters, the factories field by field, the containment law of the String and byte[] projectors, the
  bitmap algebra, refusals and the progress / cancellation protocol over table sizes 0..16,449 rows (258 words).
* `differential.sh`: the donor subset (true donor files; the projector factory without its search-family members;
  `NativeSignalOps` verbatim, without a library) against the relocated classes: the oracle output, a dump driver
  (`differential/M3ObjectSignalDump.java.tmpl`, one template in the donor vocabulary, `gen_dump.py` writes both sides)
  under the default JIT and `-Xint`, and `javap -p`.
* `mutants.sh`: seven one-line mutants, each must be killed by the oracle, the model or the donor dump.
* `verify.sh` runs every gate; results in [GATES.tsv](GATES.tsv), lessons in [RECIPE_MEMORY.tsv](RECIPE_MEMORY.tsv).

## Next leaves (not in this PR)

* The JNI kernel next to `src/main/native/collections/m3_signal_order_jni.c` (opt-in), with its four seams: done by
  `../object-signal-native-20261107` (relocate.py v2: KEEP rows, C FILE row, library m3signalorder).
* `immutableId()` over `M3ImmutableId`; `byCanonicalBytes` over an M3 codec once the codec family lands.
* The search-family projectors and the com.synexia `MatIndexObjectSignal` family (owner: m3/ports/indexstring).
* The `m3-object-projection` gate family (its own scope call).
