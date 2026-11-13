<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Existing M3 collection JNI verification

PASS: isolated actual-source Java 21 compilation and existing C/JNI verification. No production source was edited by this lane. Sources were copied from the receiving module hydrated from public `hsoliwal/M3jdk21` commit `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`. Every copied source has a SHA-256 and Git blob ID in `source-hashes.json`. A post-run comparison found no changed copied source at report generation.

Compiled 134 production Java sources using Temurin 21.0.12.1+1 with `--release 21 -proc:none -Xlint:all,-module -Werror`. Compiled 10 incumbent C sources into 9 shared libraries using GCC 13.3.0 with `-std=c17 -O2 -Wall -Wextra -Werror -Wpedantic -fsanitize=undefined -fno-sanitize-recover=all -fPIC`, JDK headers and generated JNI headers. No compiler warnings or sanitizer/JNI diagnostic occurred in successful gates.

## Reproduce

```sh
python3 proof/native-collections/run_native_verification.py
python3 proof/native-collections/verify_capabilities.py
```

Commands use the explicit JDK directory `/tmp/m3-toolchain/jdk-21.0.12.1+1`. Exact expanded argument arrays, exits, durations and complete process output are in `commands.json` and `logs/`. Build output stays under this proof directory. The module `target` and `out` directories are not used. Historical `verify.py` and `verify_donation.py` were read for owner instructions but not invoked because they write shared module output and enforce historical inventories.

## Checked-JNI results

All entries below ran with `-ea -Xcheck:jni`; the existing rank and signal opt-in properties were enabled and the actual compiled libraries were on `java.library.path`. Counts are the oracle-reported assertion totals, not the number of native transitions or unique independent cases. Counts overlap and must not be added.

| Oracle | Output |
| --- | --- |
| M3IndexMaskCompactorSelfTest | PASS M3IndexMaskCompactorSelfTest checks=26152 JNI=true |
| M3IndexMaskWindowSelfTest | PASS M3_INDEX_MASK_WINDOW checks=30596 native=true |
| M3IndexMaskWindowRecoverySelfTest | PASS M3IndexMaskWindowRecoverySelfTest checks=10528 JNI=true |
| M3MaskNativeParityProof | PASS M3MaskNativeParityProof checks=11520 checkedJNI=true |
| M3NativeIndexRangeSelfTest | M3NativeIndexRangeSelfTest mode=range checks=638454 seededCases=2048 |
| M3FilteredListsMaskBridgeProof | PASS M3FilteredListsMaskBridgeProof checks=16241 native=true |
| M3IndexPathMasksSelfTest | PASS M3IndexPathMasksSelfTest checks=2324 JNI=true |
| M3LongIndexPathMasksSelfTest | PASS M3LongIndexPathMasksSelfTest checks=7127 randomized=1500 |
| M3IndexPathMaskWindowSelfTest | M3_PATH_MASK_WINDOW_PASS checks=22461 backend=JAVA nativeAvailable=true |
| M3IndexPathRankNativeSelfTest | PASS M3IndexPathRank JNI parity: 180 deterministic Java/rank/native paths |
| M3LongAddressKernelSelfTest | PASS M3LongAddressKernelSelfTest checks=384942 native=true |
| M3LongAddressBoundedSelfTest | M3_BOUNDED_GATHER_PASS checks=21975732 fingerprint=-8730037866241443045 native=true |
| M3LongRankIndexSelfTest | M3LongRankIndexSelfTest PASS native=true |
| M3SignalOrderContract | PASS M3SignalOrderContract backend=NATIVE_REQUIRED checks=2010 |
| M3SignalDimensionsNativeContract | PASS M3SignalDimensionsNativeContract JNI checks=400 |
| M3SpanIndexSelfTest | M3SpanIndex Java21/JNI oracle: PASS 128804 assertions, JNI=true |
| M3StateMaskHistogramSelfTest | M3StateMaskHistogramSelfTest PASS checks=354665 native=true |
| M3ResidencyStateMaskProof | M3_RESIDENCY_STATE_MASK_PASS checks=11226 fingerprint=12197969927824375845 backend=jni-enabled |
| M3LongRangeSetSelfTest | PASS M3LongRangeSetSelfTest oracleChecks=411913 |
| M3LongSortedSnapshotSelfTest | M3_LONG_SORTED_SNAPSHOT_PASS / assertions=472524 / randomRounds=256 |
| M3LongBitmapFilterPlanSelfTest | PASS M3LongBitmapFilterPlanSelfTest checks=637172 native=true |
| M3LongBitmapFilteredSnapshotSelfTest | PASS M3LongBitmapFilteredSnapshotSelfTest checks=32425 native=true |
| M3StateMaskArraysSelfTest | PASS M3StateMaskArrays checks=157638 JNI=true |
| M3SdkRecoverySelfTest | PASS M3SdkRecoverySelfTest checks=19834 native=true |
| 08-segmented-bit-lane-jni | JNI parity PASS: 66 boundary ranges, 1200 changing random ranges, checked JNI |
| 10-actual-native-capabilities | rangeAbiNative=1; positionsAbiNative=1; PASS native owner availability=8, actual ABI version checks=2 |
| 11-path-mask-window-native | M3_PATH_MASK_WINDOW_PASS checks=22462 backend=NATIVE_REQUIRED nativeAvailable=true |

`M3LongIndexPathMasksSelfTest` and `M3LongSortedSnapshotSelfTest` are Java-only additional regression oracles; starting their JVM with checked JNI does not make those assertions JNI comparisons. The first path-window run selects JAVA; the supplemental run explicitly selects NATIVE_REQUIRED. Other automatic availability branches reported native present; an additional witness checked all 8 availability-reporting native owners and called both private ABI capability functions through Java reflection. The segmented bitmap library is separately explicitly loaded by its original probe.

## ABI and schema

All 17 native method declarations generated by actual Java 21 `javac -h` match the 17 exported `Java_...` symbols from `nm -D --defined-only`, with zero missing or unexpected symbols. Generated headers are in `headers/`; actual export logs are in `logs/06-*`. Actual JNI calls return `M3IndexMaskCompactor.rangeAbiNative() == 1` and `positionsAbiNative() == 1`. No JNI declaration or C owner was changed. Native APIs exchange existing primitive arrays and scalars; no predicates, comparators, GC object addresses or native collection wrappers were added.

`M3_EXISTING_COLLECTION_NATIVE_VERIFICATION_V1` and `M3_EXISTING_COLLECTION_JNI_SYMBOL_VERIFICATION_V1` are local evidence-record schemas, not new production wire formats. `jni-abi.json` lists every symbol, capability result and built-library SHA-256.

## Native source pins

| Source | Git blob | SHA-256 |
| --- | --- | --- |
| m3_index_mask_compactor.c | `aaa74686949de59b44b363741ef1c414fe3e95d0` | `8610e9fcedcc8eb698acbf3161bca8ea7d16194e61c66915f9e25fe47aae27f7` |
| m3_index_mask_window.c | `e621934162b1e6b34a1749841f6183ee4873f132` | `e57a418a183326d17cd117bcb81c2ddd49202b8195778f38a06b970c1058f16d` |
| m3_index_path_masks.c | `69aef883c09997bae30825d7ad8757bc2358bf69` | `072e6c3d30b1fd54d8ce883ed18fde75aa6c1809a1dc0c9a2de7b7e5f571f824` |
| m3_long_address_kernel.c | `9f68bba46827db2cb53bdb597042829358663f91` | `ba2e8a66c23bffa52e1b9e20eb9f1e890fb41ee13891dae0ed8d48cd6069f5ba` |
| m3_long_rank_batch_jni.c | `eacc444a737a797a43a1a559f936c55fc29bb5e1` | `75fca9fea6741cb1b13e1b96fcc34d560a5e3bba3ee59b3b485d1ca69b72aee0` |
| m3_residency_state_mask.c | `7fc64faa35977cd87f19dacea23cb60d48f88107` | `0b066dda6e0014781ace525347db3d9de6ecfb45bbd717a92240846ac4ac8fbd` |
| m3_signal_order_jni.c | `46b578b94710d804dd4eb198a8c235fb3db92547` | `e7a5991f5be2e0bad99400ff477a1c03ec9d1d8e1236f9faf366485f94966ed5` |
| m3_span_index_jni.c | `5664098cc69800fc83140a84db48cac98625232f` | `f33781ca270dda25553986da350d4d5fc3808c483363c93a7bad64717effaf3c` |
| m3_state_mask_histogram.c | `fb608faed315aded73780821d1ee7fb1b28e346d` | `0673b98601d4155ac2ea2d51f972e6f23f89e7d6c3a6105d6efd84b89fce9c1d` |
| segmented_bit_lane.c | `0d1a6f61897a6a5f3a600df0b426100156364e57` | `3567f3eed5ab042b5d88574da8f77cce967c3f35bcaf1b99d2c373af4d4ef674` |

## Limits

This is standalone current-source verification, not a historical receipt-admission verifier, Maven/OpenRewrite SDK result, full repository build, OpenJDK image/jtreg/JCK qualification, cross-platform JNI check, GC collector matrix, performance/retained-heap benchmark or whole-collection lock-free proof. Java-only sorted/filtered set contracts remain owned by the main change lane. The existing optional native kernels are bounded primitive operations, not implementations of arbitrary Java predicate/comparator execution.
