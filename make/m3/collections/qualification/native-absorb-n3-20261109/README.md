<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# NATIVE_ABSORB N3: synexia-common packed C (2026-11-09)

This leaf absorbs the C side of five jdk.m3.collections owners from com.synexia develop `537b10a9de1f`,
`synexia-common/src/main/native/collections`. `NATIVE_RELOCATE.tsv` records the blobs. The tool is
NATIVE_SYMBOL_RELOCATE v1.1, which also copies a shared header that has no JNI names.

| M3 owner | Donor C |
|---|---|
| packed.PackedArrayNative | packed_array_bulk.c (+ packed_selection_jni.h) |
| packed.PackedSelectionNative | packed_heap_selection.c, packed_sorted_sets.c, packed_stable_filter.c |
| packed.PackedLongMaskPageNative | packed_long_mask_page.c |
| packed.PackedSwissNative | packed_swiss_probe_jni.c |
| range.SegmentedShortLane28 | segmented_short_lane.c, lines 1-148 |

## Library

None of these owners loads its own library. The host loads one file, and `ArrayStorageNativeContractTest` passes it
as `m3.native.path`; that test then calls both `PackedArrayNative` and `BitLane28.cardinalityNative`. This matches
the donor packaging, so make/m3/collections builds one library, `m3_collections_packed`, from the files above plus
`segmented_bit_lane.c`. That C file is therefore linked into both `m3_collections_packed` and `m3bitlane`.
The `m3-collections-native` profile sets `m3.native.path` to the new library. Among the tests surefire runs, only
`ArrayStorageNativeContractTest` reads that property.

## Declared deviations from the donor

- **`segmented_short_lane.c` keeps only lines 1-148.**
  - Lines 149-261 are an appendix for `com.synexia.m3.residency.M3FlagPageQuery`, and M3 has no such owner.
  - The appendix itself says the lines above it are the original source, "retained byte-for-byte".
  - It can be absorbed once its owner lands.
- **MinGW only: `-Wno-float-conversion` for `m3_collections_packed`.**
  - MinGW's `signbit()` macro expands its float branch even for a double argument (`ps_compare_Double`).
  - glibc uses a type-generic builtin, and on Linux the library builds with the full flag set.

## Census v1.2

The selection and array kernels define their JNI functions through token-pasting macros
(`Java_<owner>_bounds##NAME##0`). Census v1.1 read only literal symbols, so it reported those methods ABSENT even
though the build exported them. Export_check showed the gap as 64 symbols the binary exported but the census did
not expect. v1.2 reads the `##NAME##` templates.

| | After N2 (census v1.1) | After N3 (census v1.2) |
|---|---|---|
| BOUND | 116 | 186 |
| ABSENT | 87 | 17 |

The 17 methods still ABSENT, which are the remaining N2-N7 leaves:
- the Mat kernels: MatBitMatrix 2, MatCsrGraph 1, MatIntFilterKernel 1, MatIntPostingIndex 7, MatIntRowKernel 2;
- MemoryPressure 2;
- MarkerTopKNative 1;
- IndexMaskCompactor.firstEqualBits0 1.

## Gates

See GATES.tsv.
