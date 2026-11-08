<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Packed integers and VInt attribution

Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.

This supplement applies to the packed integer and canonical VInt additions in
the independently licensed Apache-2.0 `com.m3.arrays` module. The original
module [LICENSE](LICENSE) and [NOTICE](NOTICE) retain their existing scope and
text. OpenJDK and other contributors' license and notice obligations remain
unchanged.

## Exact source custody

Canonical implementation repository: `hsoliwal/com.synexia`.
Source revision: `6f878f9e5d6c5044eda15efb9e743594e70ce7f7` (original packed-integer PR 13955).
The target receiving baseline is
`hsoliwal/M3jdk21@669a949d192345acefcfc5e9bb0b3724174d270c`.
The following raw SHA-256 identities bind the donor source bodies. They do not
claim target execution, ABI or installed-image acceptance.

| Donor path | Source SHA-256 |
| --- | --- |
| `synexia-common/src/main/java/com/synexia/packed/PackedCodec.java` | `0c953b140cf247ab804a8b79ebbde90a7a3f1ab61fdc17ec3e581c9f4a09a636` |
| `synexia-common/src/main/java/com/synexia/packed/PackedIntArray.java` | `fb30010804e8f5e48d4194d2628e670de42704cd0570e677724bdb68eda7c195` |
| `synexia-common/src/main/java/com/synexia/packed/Int40Array.java` | `b3d389aaf7375027ed47ba0055e7a3727fc6fcf820aed2580408862584f0b55a` |
| `synexia-common/src/main/java/com/synexia/packed/Int48Array.java` | `7e12bfeaa2619e235206d6ba60734d3ca30c8a0fcce57b7ca9b49881d618d952` |
| `synexia-common/src/main/java/com/synexia/packed/Int56Array.java` | `82ff070afb2c10b1da101d6bff350c8f91f617080bda6003ddf6f7a034163ee0` |
| `synexia-common/src/main/java/com/synexia/packed/VInt.java` | `a5d297f7735fd962426ad82089f658f4272531ec26192c218b7a71d342085291` |
| `synexia-common/src/main/java/com/synexia/packed/package-info.java` | `fac8b09009473a97208d2fc6f8f435055a0a411b5af2130f2792ba61d329bba8` |
| `synexia-classic-tools-jni/src/main/c/synexia_cpu_codec.c` | `c6bd9da6c9ede861bc481c4a8bf84e6114eeda159f1af6d4c9a27d5400a2682a` |
| `synexia-classic-tools-jni/src/main/c/synexia_cpu_codec.h` | `54e248ac99d11aa3d5742ffb882915804c2dd3527ca23c033bd315ffc7c38dcb` |
| `synexia-classic-tools-jni/src/main/c/synexia_cpu_codec_jni.c` | `e9f840dbe9af3b7798f5ad909b01bc65ab60199090406d95401a0c3cc659cad6` |
| `synexia-classic-tools-jni/src/main/java/com/synexia/classictools/jni/NativeCpuCodec.java` | `69cd26eac769e56a5c43ab310803d263d2fc596d0f9479f93cddf9b2ea358c82` |

Portable Java source and associated tests are adapted into
`com.m3.arrays.packed`, with the signed 40/48/56-bit and canonical
unsigned64/ZigZag64 contracts retained. The donor `package-info.java` is package
documentation, and `PackedCodec` remains an internal implementation helper.

Only the six relevant packed/unsigned64/ZigZag64 C kernels and bounded JNI
boundary are adapted to target-owned `m3_packed_codec.c`,
`include/m3_packed_codec.h`, and `m3_packed_codec_jni.c`. They extend the existing
`M3ArrayNative` facade and `m3_arrays` native library using target symbols,
loading and shared JNI exception helpers. Donor encode63/delta/ABI, progress
and library-loader machinery are excluded. Existing UTF-16 behavior and native
exports remain unchanged. Every modified source retains its applicable
copyright and Apache-2.0 identification.

## Algorithm and problem-category references

The fixed-width byte packing and canonical base-128/ZigZag algorithms are
established representations. LeetCode, HackerRank and GeeksforGeeks are used
for problem-category comparison only; no challenge statement, editorial or
submission body is included. The qualified donor's category and GitHub review
record remains canonical at
`tools/short-flag-query/reviews/packed-integers-20261029.tsv` in Synexia.
No third-party body is relicensed merely by review or transformation.

## Canonical transformation and target acceptance

Canonical recipe identity: `com.synexia.rewrite.M3JdkPackedIntegersRecipe`.
Canonical task: Synexia
`synexia-openrewrite-recipes/verification/m3jdk-packed-integers-20261029`.
The reusable recipes, templates, compiler/JUnit mastery corpora and execution
records remain solely in Synexia. M3JDK21 receives product outputs and compact
source/execution references, then owns its target compiler/API/ABI/JNI,
installed-module and source-built image verification.

Stable target mapping IDs: `m3.arrays.packed.signed`,
`m3.arrays.packed.vint64`, and `m3.arrays.packed.native`.
These records remain `implemented-unverified` until independently supported
by exact target receipts. This supplement does not advance a receiving phase,
claim full-JDK compatibility, or assert a universal memory or performance win.
