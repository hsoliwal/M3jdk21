<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# One owner for the 28-bit lane (NATIVE_ABSORB N7a, 2026-11-09)

## Finding

The NATIVE_ABSORB census (`../native-build-20261109`) found two classes for one concept: a lazy 28-bit occupancy
lane, in which every touched 4,096-slot block owns 64 long words.

| Class | Origin | C side |
|---|---|---|
| public `jdk.m3.collections.BitLane28` | Synexia `SegmentedBitLane28`, received as `M3BitLane28` and renamed by curation (`m3/docs/name-mapping.json` id `synexia.collections.SegmentedBitLane28`; `m3/tooling/lane28`) | `cardinality0` in `segmented_bit_lane.c` (library `m3bitlane`) |
| internal `jdk.internal.m3.collections.bits.SegmentedBitLane28` | absorbed again from Synexia m3-common by RELOCATE_M3_OWNER v4 (common collections batch) | none (its `cardinality0` was ABSENT) |

The second absorb missed the existing receipt; that batch predates the RECEIPT_MAP guard.

- **API.** `javap -public` shows that the public API of `SegmentedBitLane28` is a strict subset of `BitLane28`'s.
  `BitLane28` adds only `indexPayloadBytes()`.
- **Internals.** The two differ only in private bookkeeping: per-page counts in `SegmentedBitLane28`, Fenwick
  indexes in `BitLane28`.

## Proof of equal behaviour

`tools/LaneDiff.java` drives both classes with one random operation stream: set, clear, set(i, b) and flip. Indices
are clustered near 0 and near 2^28, spread across the full range, and include a few out-of-range values. After every
step it compares the results and exception classes of every public observation:
- get, cardinality, cardinality(from, to) in both argument orders;
- allocatedRegionCount, allocatedBlockCount, payloadBytes;
- nextSetBit, previousSetBit, rank, select.

The results are in `DIFFERENTIAL.txt`, and every check below was run on the classes of master ab022da1246d:
- **Seeds 1 to 5, 20,000 steps each:** about 1.18 million comparisons and 0 mismatching methods.
- **Mutant run:** the BitLane28 side skips flip. Eleven methods then mismatch, which shows the harness detects a
  real divergence.

## Change

- The users are retargeted to `jdk.m3.collections.BitLane28`. They are `LeanSegmentedCollections.bitLane()`, the
  `SegmentedSlotAllocator28` live lane, `CollectionFamilies.BIT_LANE_28` (family `bit-lane-28`), the
  `StandardCollectionCatalog` provider `lane.bit-28`, and `ArrayStorageNativeContractTest`.
- `jdk/internal/m3/collections/bits/SegmentedBitLane28.java` is removed. The `bits` package keeps its other 16 classes.
- One ABSENT native method leaves the census.
- The custody tip is resealed: the module-source successor is rerun, `--check` reports changed=0, and the verify
  tests pass.

Hash-pinned records that name `SegmentedBitLane28` (name-mapping receipts, lane28 tooling, migration evidence) are
left unchanged, by rule.
