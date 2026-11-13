<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# NATIVE_ABSORB N2: synexia-primitives C (2026-11-09)

This leaf absorbs the C side of six jdk.m3.collections owners whose Java was already absorbed but whose `native`
methods had no C in M3 (ABSENT in `../native-build-20261109/NATIVE_CENSUS.tsv`). The C comes from com.synexia develop
`537b10a9de1f`, `synexia-primitives/src/main/native`. `NATIVE_RELOCATE.tsv` records each file's blob.

## Recipe: NATIVE_SYMBOL_RELOCATE v1

`../native-build-20261109/tools/native_relocate.py NATIVE_RELOCATE.tsv <donor dir> <repo>` copies each donor file
to `src/jdk.m3.collections/share/native/libm3collections/collections/`. It changes only what the move requires:
- the `Java_<donor class>_` prefixes become the mangled M3 binary name;
- the donor generated-header include becomes the M3 javac -h header name;
- one lineage comment line is added.

The make/m3/collections build force-includes the M3 javac -h headers. Every relocated definition is therefore
checked against its M3 Java declaration at compile time.

| M3 owner | Donor class | Library |
|---|---|---|
| packed.PackedNative | com.synexia.primitives.M3PackedNative | m3_packed_integer (loadLibrary) |
| packed.PrimitiveBigNative | com.synexia.primitives.PrimitiveBigNative | m3_big_longs (loadLibrary) |
| DoopFrozenMaskedViewport | com.synexia.primitives.M3DoopFrozenMaskedViewport | m3_doop_frozen_mask (loadLibrary) |
| DoopMaskedWindowScan | com.synexia.primitives.M3DoopMaskedWindowScan | m3_doop_mask_window (loadLibrary) |
| PrimitiveFloat16 | com.synexia.primitives.PrimitiveFloat16 | m3_value_bits (host-loaded) |
| PrimitivePartialSort$LongOrder | com.synexia.primitives.PrimitivePartialSort$LongOrder | m3_primitive_order (host-loaded) |

## Build changes

- **`m3_value_bits` needs IEC 60559 floats.** The donor guard `#error` fires without `__STDC_IEC_559__`, and the guard
  is kept. CMake checks for the macro and builds the library only where it holds (glibc Linux yes, MinGW no). Skipped
  classes go to `<build>/m3_skipped_jni_classes.txt`, which export_check v1.1 leaves out of the expected set.
- **Nested classes.** `m3_collections_jni` accepts a nested class written `Outer$Inner`: the header is
  `Outer_Inner.h` and the source is `Outer.java`. Every library registers itself, so the header dependency needs no
  hand list.
- **Tools.**
  - native_census v1.1 attributes natives of nested classes to `Outer$Inner`. v1 credited
    `PrimitivePartialSort.LongOrder.nativeAdvance` to the top-level class and so expected the wrong symbol.
  - native_libmap now reads nested owners.
  - build_check takes a census override, `M3_NATIVE_CENSUS`.

## Census after N2 (NATIVE_CENSUS.tsv, NATIVE_LIBS.tsv)

| | Before | After |
|---|---|---|
| Declared native methods | 204 | 203 (N7a retired one) |
| BOUND | 105 | 116 |
| ABSENT | 99 | 87 |

## Gates

See GATES.tsv.
