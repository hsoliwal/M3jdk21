<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# object-signal-native-20261107: the JNI kernel of the object-signal plane pass, in libm3signalorder

Follow-up of `../object-signal-20261107` (#2686, merged as 3654bf4265b2), which relocated the object first-pass
signal family Java-only and HELD the donor's JNI kernel (its DECISION.tsv D7). This leaf relocates the kernel and
restores the seam.

## What changes

| Path | Change |
|---|---|
| `src/main/native/collections/m3_object_signal_jni.c` | the donor kernel (m3scale `m3-object-signal/src/main/c/object_signal_jni.c`, blob 4c3bcedc7980), relocated by `../object-signal-20261107/relocate.py` v2: M3JDK header, one anchor, the JNI prefix `Java_jdk_m3_collections_M3ObjectSignalOps_`; every other byte is the donor's (its local `trailing_zero_u64` helper included) |
| `M3ObjectSignalOps.java` | the JNI seam again (the four DROP_BLOCK rows of #2686 are KEEP rows): `JNI_WORD_THRESHOLD = 256`, the opt-out loader (`m3.objectsignal.disableNative`), `available()`, the native dispatch for 256 words and more, the native declaration; the library name is `m3signalorder` (one ADAPT row) |
| `M3ObjectSignalSelfTest.java` | the donor PASS line with its `JNI=` flag (the REWRITE row of #2686 is gone) |
| `M3ObjectSignalNativeTest.java` (new, test) | dispatching kernel vs Java kernel over 0..1,024 words; the library is required under `m3.collections.all.native.required` |
| `qualification/full-java-jni-20261018/verify.py` | the module's JNI library registry: `m3signalorder` = `m3_signal_order_jni.c` (M3SignalOrderNative) + `m3_object_signal_jni.c` (M3ObjectSignalOps) (`apply.py` N1) |
| `JDK_FAMILY_MAP.tsv` | the object-signal row names the JNI kernel (`apply.py` N2) |

No pom.xml, module-info.java, CMake or workflow change. Decisions are in [DECISION.tsv](DECISION.tsv):
- N1: the kernel goes into the existing library `m3signalorder`, not a new `libm3objectsignal`.
- N3: the donor's opt-out loading policy is kept.
- N4: m3_jni_util.h is not used. Leaf-50 decision #2694 is KEEP_LOCAL: the module compiles native code with
  `$JAVA_HOME/include` and the `javac -h` directory only.

## Recipes

```sh
python -I make/m3/collections/qualification/object-signal-20261107/relocate.py --donor-git <m3scale clone> \
    --donor-rev 3385d79a2968c770aedf1d3398b2a62f05ebc092 [--check]       # v2: KEEP rows, C FILE row, JNI row
python -I make/m3/collections/qualification/object-signal-native-20261107/apply.py [--check]
bash make/m3/collections/qualification/object-signal-20261107/differential.sh <m3scale clone> 3385d79a2968 <scratch> 20000
bash make/m3/collections/qualification/object-signal-native-20261107/native-gate.sh <out> <scratch> 20000   # Linux / WSL
```

`native-gate.sh` checks:
- the `javac -h` prototypes against the C definitions;
- five gcc builds with 0 diagnostics (release `-Werror`, the verify.py flags with UBSan, the kernel without
  `-include`, the master library, and the donor library with the donor CMake flags);
- the `nm -D` exports;
- eight dumps of the 20,000-query differential driver, which must be byte-identical: relocated Java and native
  (default JIT and `-Xint`, `-Xcheck:jni`), native with the UBSan library, native disabled by the property, and
  donor Java and donor native, all also compared with the Windows donor dump;
- the native link (`-Xlog:jni+resolve`);
- the oracle and reference model on both kernels;
- five C mutants.

Results are in [GATES.tsv](GATES.tsv) and lessons in [RECIPE_MEMORY.tsv](RECIPE_MEMORY.tsv).
