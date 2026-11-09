<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# jdk.m3.collections JNI build (NATIVE_ABSORB N1, 2026-11-09)

## Problem

Until this change, nothing built the C sources in `src/jdk.m3.collections/share/native/libm3collections`:

- `make/m3/arrays/CMakeLists.txt` builds only `libm3arrays`.
- `m3/native/CMakeLists.txt` still named the sources by their pre-move paths, `m3/arrays/src/main/native` and
  `m3/collections/src/main/native`. Neither path exists after #2849, so its CMake configure step failed.
- `m3/collections/viewport-proof` also points at pre-move paths. It is left unchanged here (see Open items).

`tools/native_census.py` compares every Java `native` method of the module with the C symbols that exist.
`NATIVE_CENSUS.tsv` and `NATIVE_LIBS.tsv` hold the result for 45 classes:

| Methods | Count |
|---|---|
| Declared native | 204 |
| BOUND (a C symbol exists) | 105 |
| ABSENT (no C symbol anywhere in M3) | 99 |

Of the 105 BOUND methods, 70 belong to jdk.m3.collections classes and are served from libm3collections. The other
35 belong to jdk.m3.arrays classes and are already built by `make/m3/arrays`. The 99 ABSENT methods are leaves N2 to
N7 of the plan.

## Change

1. **New `make/m3/collections/CMakeLists.txt`.** It builds 14 shared libraries. Each is named exactly as its Java
   owner loads it (`LIBRARIES.tsv`).
   - On Windows the libraries carry no `lib` prefix, so `System.loadLibrary` finds them.
   - One `javac -h` run generates the JNI header of every declaring class. Each library force-includes the headers
     of the classes it serves. A C definition whose signature drifts from its Java declaration then fails with
     "conflicting types" (`tools/mutant_check.sh`).
   - Only the JNI headers are used: no AWT, and no libjvm is linked.
   - Symbols are hidden by default, so only the `JNIEXPORT` entry points are exported.
2. **Two stale includes left by the moves are fixed.**
   - `m3_index_path_masks.c` now includes `"m3_jni_core.h"`, found through the `m3/native/include` include
     directory as in `make/m3/arrays`. It previously used a relative path that no longer resolves.
   - `m3_residency_state_mask.c` now includes the current generated header,
     `jdk_internal_m3_collections_ResidencyStateMaskIndex.h`. It previously named the pre-rename header
     `jdk_m3_collections_M3ResidencyStateMaskIndex.h`.
3. **`IndexMaskWindow` joins the `m3_collections_mask` library.** The class loads nothing itself: it calls its
   natives once `IndexMaskCompactor.nativeAvailable()` reports `m3_collections_mask` loaded, as its Javadoc states.
   In the first native-profile run, with `IndexMaskWindow` in its own library, `IndexOrderWindowTest` and
   `ObjectSlotIndexTest` failed with UnsatisfiedLinkError.
4. **`m3/native/CMakeLists.txt` is repointed** to the module layout. It now configures and builds, and its ctest passes.
5. **The `m3-collections-native` profile in `m3/collections/pom.xml` is extended.**
   - `-Djava.library.path=${m3.collections.jni.dir}` is set in argLine, because `System.loadLibrary` reads it only at
     JVM start.
   - `m3.counted.native` and `m3.totals.native` are set for the host-loaded count kernels.
   - Some owners load their library only on opt-in. The profile turns these on: `m3.collections.rank.native`,
     `m3.collections.rank.pairdistance.native`, `m3.collections.signal.native` and `m3.collections.lane.native`.
   - It also sets the strict gates `m3.collections.all.native.required` and `m3.mask.spans.native.required`. With
     them, `FilteredSplitsMaskSpansTest` asserts that all nine built backends report available, and
     `ObjectSignalNativeTest` runs instead of skipping.
   - The new properties `m3.collections.jni.dir` (default `${m3.collections.native.dir}`) and
     `m3.collections.jni.prefix` (default `lib`) are added.
6. **`NativeFiveLibrarySdkNoVendorTest` (excluded by default, runs under the profile) is fixed.** It looked for
   `src/main/java/jdk/m3/collections`, which #2849 moved. It now reads
   `../../src/jdk.m3.collections/share/classes/jdk/m3/collections`.
7. **Custody is resealed in the same change.** `module-source-successor-20261109.py` is rerun because
   `m3_index_path_masks.c` is a recorded source. `--check` then reports changed=0.

## Running it

```
# Linux
JAVA_HOME=<jdk21> bash m3/collections/qualification/native-build-20261109/tools/build_check.sh . /tmp/m3coll
# Windows (MinGW)
JAVA_HOME=<jdk21> bash m3/collections/qualification/native-build-20261109/tools/build_check.sh . <dir> -G Ninja -DCMAKE_C_COMPILER=gcc
# Java side: build make/m3/arrays too, then
mvn -f m3/collections/pom.xml verify -Pm3-collections-native -Dm3.collections.native.dir=<arrays build> \
    -Dm3.collections.jni.dir=<collections build>
#   on Windows add -Dm3.collections.native.suffix=.dll -Dm3.collections.jni.prefix=
```

## Gates

See `GATES.tsv`.

## Open items

- **CI.** No workflow builds `make/m3/collections` yet. The obvious place is a step in
  `collections-convergence.yml` next to the `make/m3/arrays` build.
- **Skips that remain under the profile.**
  - `PackedNativeTest` (130 tests): the packed natives are ABSENT. They are leaf N2.
  - `MatBitMatrixTileJUnitTest` (1 test): it needs `m3.matcollections.jni`, and MatBitMatrix has no C. It is leaf N7.
- **`m3.native.path`.** `ArrayStorageNativeContractTest` and the `MaskViewport*Contract` mains both read this one
  property, but they need different libraries. ArrayStorage needs the packed-array natives, which are ABSENT (N2).
  The profile therefore leaves the property unset.
- **`m3/collections/viewport-proof`.** It and `verify_mask_viewport.py` still name pre-move paths. Retiring them in
  favour of this build, or repointing them, is a later leaf.
- **N2 to N7.** These leaves absorb the com.synexia C for the 99 ABSENT methods into this CMake, and hunt for or
  retire the five classes that have no C anywhere (`nativecrate/PLAN.md` in the com.synexia recipe crate).
