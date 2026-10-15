<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Retained shared-array source coverage

[Guide](README.md) · [Ownership and JNI reference](retained-shared-array-reference.md) · [Custody package map](packages/custody-01.md)

## Scope and evidence standard

Public repository: `hsoliwal/M3jdk21`.
Source pin: `363c02caa2e5af1df9460b927ecd78f6a9dbcefb`.

The eight Java files below are the complete primary body set for this reference.
Their complete declarations, nested types, private support paths, validation,
error and ownership behavior were read. Both C++ companions were read in full
as supporting JNI evidence. The hashes identify the exact public receiver
bytes; they are not a semantic proof, license-grant assertion or build result.
The retained module is incomplete and has no own `pom.xml` or `module-info.java`.

The [shared-array intake manifest][provenance] records all eight Java bodies,
the CMake input and both C++ files as `PINNED_VENDOR_CUSTODY`, from donor revision
`857c4cc0ddeb73e5ea5269a1d0ef2d30057dcfa8`. Their current Git blobs match that
public provenance. The mapbox ISC item is explicitly
`PINNED_LICENSE_NOTICE_ONLY`; `jni/ownership.hpp` was not copied. Reading those
public records is not verification of the donor repository or any omitted
source. The [target map][target-map] preserves candidate/owner-mapping status
and the prohibition on a parallel runtime descriptor owner.

The eight primary bodies contain 1,286 source lines in total. Native support
adds 575 lines, separate from that primary denominator. `shared_arrays.cpp`
has 472 logical lines, including its final line without a terminating newline.

## Exact primary identities and title-to-owner map

All source filenames in this table are under
`m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/`.

| Primary source | Lines | Contract sections | Git blob SHA-1 | Raw-byte SHA-256 |
| --- | ---: | --- | --- | --- |
| [ArrayHandle.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/ArrayHandle.java#L1-L15) | 15 | 2: handles | `610b139f44f05bf7a6bfedeb10846d29dc16b4b2` | `e9962d8db43277db6647fdeac883ac90ca460e6a4d2ab20efebd98244d04b18d` |
| [SharedArray.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArray.java#L1-L34) | 34 | 2: identity; 5: output | `d87cebc7101b5b272ff80c2b56a49b7e42fb18cf` | `7353559e1610fbf5710e75b52bae72388de992d531a01cb18baf361dc697672f` |
| [SharedArrayConversions.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayConversions.java#L1-L74) | 74 | 6: result-handle LRU | `8e6d0acd184a8bc356a49efc363c4e39530b5bf3` | `a86f1ff2efa5198fc4ea83e7267f014f04881bb785873d9aeafd0a6a0fc5c5e7` |
| [SharedArrayNative.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayNative.java#L1-L110) | 110 | 7–8: JNI routing/export | `62c2e6e089b6a203b22d5aae4e15a834876f08d1` | `bb00394e4383299e7119270047977bdb5192068fc38bf9d60fd3fb5ee109a243` |
| [SharedArrayPool.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L1-L683) | 683 | 3–4: arena/admission; 6: conversion; 9: geometry/lifecycle | `e0207b70c51f9369fdc0b617698e31ba5014db88` | `b316c3687b4695612d5081c8b6ff89090d6180370b815088a51f6bb40b7dab82` |
| [SharedBytes.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedBytes.java#L1-L35) | 35 | 2: bytes; 5: slices/copies | `372ff3ded11b4067bd7896cb8543b448ae82403a` | `294f498851d66f5662953f6b0c8a2f45f1dbf9ae6f16adc6bfbc9d054d835d58` |
| [SharedChars.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedChars.java#L1-L58) | 58 | 2: characters; 5: slices/copies | `f05b41bf87472219e5a602efcc62858a903d15a5` | `ab0392e409b877a445e48a436794ce97785f0a37cfa149b89e22c071d4910a68` |
| [SharedSegments.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedSegments.java#L1-L277) | 277 | 4–5: geometry/cursors; 8: range plans | `2912e2afbc13a981ed6f6ef704b8b17047481fea` | `625f7ed393237749fb88710b12d53a82243afcee03f4a1db209c87629d886d61` |

## Complete Java member routes

Ranges below are bounded source links at the pin. They account for declared
members of the eight bodies, including package-private support. Inherited
methods whose source interfaces are absent from this retained subtree are not
silently counted as independently reviewed bodies.

| Owner and members | Source range | Behavior route |
| --- | --- | --- |
| `ArrayHandle` record components, compact constructor and generated accessors/value methods | [8–14][handle-body] | Namespace/record/range value; minimum offset and checked nonnegative `int` range |
| `SharedArray` fields and package-private constructor | [7–14][array-body] | Immutable handle, segment directory, language and cached hash references |
| `handle`, `length`, `languageId`, `segmentCount`, `asReadOnlyBuffers`, `hashCode` | [15–20][array-access] | Accessors and caller-owned descriptor views |
| `equals`, `rangeHandle` | [21–33][array-equality] | Same concrete type plus exact content; relative handle composition |
| `SharedBytes` constructor, `byteAt`, `slice` | [11–17][bytes-slice] | Byte read, full-slice identity and range metadata/hash |
| `SharedBytes.copy`, array `copyTo`, buffer `copyTo` | [18–34][bytes-copy] | Fresh mutable output; bounds/capacity/read-only preflight and target cursor effects |
| `SharedChars` constructor, `charAt`, `subSequence` | [13–21][chars-slice] | UTF-16 units and retained range geometry |
| `SharedChars.copy`, array `copyTo`, `contentEquals` | [22–35][chars-copy] | Fresh output, checked ranges and cross-type unit comparison |
| `asReadOnlyCharBuffers`, `toString`, buffer `copyTo` | [36–57][chars-buffer] | Explicit BE read-only views, String materialization, target-only cursor advance |
| `SharedSegments` fields, constructor, `length`, default `unitAt` | [9–39][segments-state] | Primitive geometry, checked positive ranges and cumulative lengths |
| `LookupCursor` fields, `binarySearches`, `locate`, `install`; cursor-taking `unitAt` | [40–93][segments-cursor] | Weak thread-local locality and unsigned byte/BE UTF-16 decoding |
| `slice`, `join` | [94–132][segments-join] | Participant-only slicing; pre-coalescing segment budget; contiguous-ID coalescing |
| `Utf16RangePlan` fields/constructor, `utf16RangePlan` | [133–167][segments-plan] | Ephemeral owner/metadata aliases and exact participating-window indexes |
| `preparedBuffers` field, `buffers`, `preparedBuffers()` | [169–196][segments-buffers] | Fresh public read-only views versus private lazily published JNI descriptors |
| Four `copyTo` overloads and `firstSegment` | [198–238][segments-copy] | Segment-by-segment transfer, target-only cursor changes and zero-count handling |
| `hash32`, `hash31Power` | [239–276][segment-hash] | Signed-byte/unsigned-char polynomial; exact same-buffer/range reuse |
| `SharedArrayPool` constants/state, `Stats`, `Fingerprint`, `Mapping` constructor, `Input` and its `segments` default, `IoAction` | [32–73][pool-state] | Arena identity, budgets, primitive indexes, optional strong/weak residency, serialized scratch |
| `open`, private constructor, `namespace` | [75–123][pool-open] | Header creation/check, exact persistent-budget identity, channel cleanup |
| `copyLiteralGeometryPage`, `geometryNextRow`, `geometryCount`, `packGeometryPage` | [125–187][pool-geometry] | Bounded emitted rows, descriptor skipping, file coordinates and packed cursor |
| `locked`, `ensureOpen`, `drainMappings` | [188–207][pool-lock] | Class-instance-domain JVM serialization, process lock, refresh and identity-safe weak cleanup |
| Three `internBytes` overloads | [209–237][pool-bytes] | Array/buffer ingress; same-namespace attachment versus foreign import |
| Two `internChars` overloads, `charInput`, `lookupChars` | [238–260][pool-chars] | Language-aware admission, same-namespace relabeling, lookup without append |
| `concatChars`, `concatBytes`, `join` | [261–279][pool-join] | Null/namespace refusal and leaf-range geometry |
| `encode`, `decode` | [280–295][pool-conversion] | Whole-sequence encoder and intentional decode flattening |
| Both `chars` and both `bytes` getters, `requireNamespace`, `get`, `getKnown` | [296–324][pool-get] | Width/offset/range resolution, known-row path versus refreshed unknown-row path |
| `stats`, `descriptorIndexHits` | [325–331][pool-stats] | Current instance counters after locked refresh |
| `intern`, `flushPayload` | [332–389][pool-append] | Exact descriptor shortcut, content lookup, miss quotas, append/CRC/force/commit |
| `fingerprint`, `find` | [390–423][pool-fingerprint] | Width/language SHA domain, Java hash, candidate filters and exact content check |
| `findDescriptor`, `descriptorEquals` | [424–456][pool-descriptor] | Signature filter plus exact literal-ID/offset/length tuples |
| `descriptorSignature`, `descriptorSignatureStart`, `descriptorSignatureAdd`, `mix64`, `slot` | [458–498][pool-signatures] | Non-authoritative metadata signatures and open-addressing slot routing |
| `add`, `insertSlot`, `insertDescriptorSlot` | [499–560][pool-index] | Primitive index growth, counters and rehashing |
| `refresh`, `validateDescriptors` | [561–617][pool-refresh] | Limited incomplete-tail truncation; structural, CRC, commit and literal-reference refusal |
| `view` | [618–658][pool-mapping] | Weak literal mapping reuse, strong local promotion, descriptor-owner assembly and bounded view LRU |
| `read`, `write`, `close` | [659–682][pool-close] | Exact progress/EOF errors and idempotent channel/cache close, without explicit unmapping |
| `SharedArrayConversions` state, private `Key`, public `Stats`, constructor | [17–31][conversion-state] | Caller-owned pool and count-bounded result-handle cache |
| `encode`, `decode` | [33–59][conversion-ops] | Stable-standard identity, bypass/hit/miss order, resolving hits through pool lifecycle |
| `stats`, `clear`, `close`, `ensureOpen`, `retain`, `stableStandard` | [61–73][conversion-lifecycle] | Counter/entry snapshots, handle eviction, closed-facade behavior, six singleton charsets |
| `SharedArrayNative` replacement constant/private constructor, `load`, both `hash32`, `compare`, `contentEquals`, native compare/hash declarations | [13–39][native-entry] | Explicit loading and private complete-capacity descriptors |
| `metrics`, `scanUtf16Segments`, `scanUtf16Segments0` declaration | [40–49][native-metrics] | Eight-slot metric result and JDK UTF-8 encoder replacement width |
| Two `copy` and two `copyTo` overloads | [51–94][native-export] | Java preflight, caller-owned output, zero-count bypass and range-plan JNI dispatch |
| `copyCheckpoint`, `copyUtf16SegmentsTo0`, `copyUtf16RangeTo0` declarations | [96–109][native-declarations] | Interrupt/monitor checks, retained unused complete-export declaration and active range declaration |

## Native support identities and complete routes

The two C++ files and CMake input are under
`m3/vendor/synexia/synexia-indexstring/native/shared-arrays/`.

| Supporting source | Lines | Git blob SHA-1 | Raw-byte SHA-256 |
| --- | ---: | --- | --- |
| [shared_arrays.cpp](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_arrays.cpp) | 472 | `8b85031cfbe1d64f43c93a1453f8670cc65dbafe` | `111fda915053bfb58f448e4e9604e18cd41cdf55c438975193df4ebe6a7cf496` |
| [shared_string_compare.cpp](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_string_compare.cpp) | 103 | `c0bf741d48304dceb73fddd0352ab510d1f2265f` | `e6ee7dbc3207698e6f62a32100deb55b9d724330b27664b3f608cffdd25ad7eb` |
| [CMakeLists.txt](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/CMakeLists.txt) | 105 | `2f2439e381f755a9834e10f417fcb41f784cb465` | `b298cb31ec06b22211055e90345e3ceca2d608a6e9403317187960daa8a07c55` |

| Native owner / helper | Source route | Evidence boundary |
| --- | --- | --- |
| `local` and `invalid` | [shared_arrays.cpp 9–27][cpp-local] | Acquired JNI local references use external mapbox ownership wrapper; inputs remain borrowed |
| `hash32Segments` export | [29–72][cpp-hash] | Width/direct-capacity validation and exact signed-byte/BE-character polynomial |
| `metric_overflow`, `utf16_segment`, `signed_bits` | [75–105][cpp-validation] | JNI exceptions, direct/even validation and bit-preserving result |
| `utf16_summary`, `scan_utf16_summary`, `append_utf16_summary` | [107–147][cpp-summary] | Invocation-local facts and surrogate seam repair |
| `utf16_cache`, nested `entry`, constructor and `get` | [149–187][cpp-cache] | Eight exact address/length summaries with local owner lifetime |
| `scanUtf16Segments0` export | [190–238][cpp-metrics] | Complete preflight, bounded locals, wide metric accumulation and eight-result layout |
| `copyUtf16SegmentsTo0` export | [240–302][native-full-copy] | Full-array export declaration/body without a Java invocation in this facade |
| `jint_array_view` constructor, destructor, `values` and fields | [305–320][cpp-meta] | Metadata `GetIntArrayElements` / `ReleaseIntArrayElements(..., JNI_ABORT)` |
| `copyUtf16RangeTo0` export | [323–459][native-range] | Full-directory preflight and participating-window block transfer |
| `JNI_OnLoad` | [460–472][cpp-onload] | JNI 1.8 environment lookup, class lookup and one explicit method registration; mismatch below |
| Comparison `fail`, `invalid`, `direct_segment`, `validate` | [shared_string_compare.cpp 6–53][compare-validation] | Both full descriptor arrays and summed-unit bounds checked before comparison |
| `Cursor` fields, constructor, deleted copy operations, destructor, `read` | [55–85][compare-cursor] | One live local per cursor, empty skipping, bytewise BE decode and local cleanup |
| `compareUtf16Segments` export | [88–103][compare-entry] | Unit difference then length difference; zero returned with pending errors is not success |
| CMake options/JNI target | [CMakeLists.txt 2–50][cmake-jni] | Conditional JNI source pair; external wrapper owner/pin and JNI headers required |
| CMake arena/test/install targets | [52–105][cmake-arena] | Separate absent C arena/test/header inputs; not evidence of runnable retained native tests |

### Native registration mismatch

The [Java declaration][native-range-declaration] has eleven parameters:
`ByteBuffer[]`, three `int[]`, three `int`, `char[]`, two `int`, and
`IProgressMonitor`. The [C++ function][cpp-range-signature] has the corresponding
JNI parameters after `JNIEnv*` and the declaring `jclass`.

The exact descriptor implied by that Java declaration is:

```text
([Ljava/nio/ByteBuffer;[I[I[IIII[CIILcom/synexia/job/IProgressMonitor;)V
```

The exact retained registration descriptor in [JNI_OnLoad][cpp-onload] is:

```text
([Ljava/nio/ByteBuffer;[I[I[I[IIII[CIILcom/synexia/job/IProgressMonitor;)V
```

The latter contains four `int[]` parameters and twelve parameters overall.
This is a source registration mismatch, not a different supported overload.
`JNI_OnLoad` looks up `SharedArrayNative`, calls `RegisterNatives` for this
single name/signature, deletes its local class reference, and returns
`JNI_VERSION_1_8` only on `JNI_OK`; otherwise it returns `JNI_ERR`. Failed
`GetEnv` or `FindClass` also returns `JNI_ERR`. There is no fallback registration.

This applies conditionally if the retained `JNI_OnLoad` is reached with this
retained Java class. The CMake `SYNEXIA_SHARED_ARRAYS_JNI` target includes this
file; the separate C arena library does not. The Java 21
[RegisterNatives specification](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/functions.html#registernatives)
defines absent name/signature failure, and the
[JNI_OnLoad specification](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/invocation.html#jni_onload)
defines the load callback boundary. No compilation, registration, load attempt,
alternative binary or repaired source was exercised by this documentation pass.

## Dependencies, callers and test accounting

- The public retained main-source tree contains these eight `shared` bodies.
  Within the retained Java bodies, pool/view/native/conversion call paths are
  routed above; no separate retained Java consumer of `SharedArrayPool`,
  `SharedArrayConversions` or `SharedArrayNative` was found outside this set
- `SharedBytes` imports `MIndexByteArrayView`; `SharedChars` imports
  `MIndexUtf16ArrayView`; `SharedArrayNative` imports `IndexTextMetrics` and
  `com.synexia.job.IProgressMonitor`. Their definitions are absent from this
  retained subtree. This reference describes only the observed calls and
  implemented methods, not an inferred complete dependency module or the
  imported types' unreviewed inherited contracts
- No shared-array test source is present in the retained `src/test` tree.
  Its five Java tests belong to AST work, not these owners. The mention of a
  focused locality proof in `LookupCursor` is not a physically supplied proof
- CMake names `test/shared_arena_test.c` and `test/fixtures/arena.bin` and arena
  C/header inputs not present in the retained native slice. Their behavior was
  not reconstructed from filenames and no C test result is claimed
- The [frozen-text fixture provenance][fixture] describes an isolated test-only
  fixture. The [separate bridge POM][bridge-pom] wires `ExistingSurfaceTest`
  and `RetainedViewTest` for its own bridge. Neither is evidence that these
  shared-array APIs compile or pass tests
- The [foundation build script][foundation-build] selects `core/src`; the
  [M3 control POM][control-pom] and [import POM][import-pom] do not establish a
  complete shared-array source module. The public custody README explicitly
  says this slice is not a complete compilable module or JDK dependency

Test status for this primary set: **no retained package tests available;
execution NOT_RUN**. Build/compiler/JUnit/jtreg/native load, replay/policy
verification, platform/cross-process/crash tests, GC/lifetime tests and
performance/memory measurements are all **NOT_RUN** in this documentation pass.
Hosted CI is separate exact-commit evidence and is not asserted as passed
by this page. Historical receipts retain their own pins and bounded scopes; no donor or
bridge receipt is transferred into current-head qualification.

## Policy, build and provenance evidence

Current [agent instructions][agents] and [contributor guidance][contributing]
were reread at the source pin. Their Git blobs are respectively
`2253be71930e69f34420a72df8e8ba278ae61fb5` and
`9fffe3af25ea1ca3f1fc13b35b922ddbad37502f`.
The read-only documentation scope does not execute their policy verifiers or
change policy, source seals, generated recipes, source comments or licenses.

The [full-family pin][family-pin], [catalogue index][catalogue],
[current successor receiver][successor], [successor pin][successor-pin] and
[promotion phases][phases] remain source/qualification evidence. They do not
advance any receiving phase. The canonical [porting invariant][policy] and
[name map][name-map] continue to govern actual target owners and aliases.
The source/target license split in the [intake target map][target-map] and
[borrowing contract][borrowing] is unchanged; no independent relicensing or
legal-permission conclusion is made here. Retained first-party attribution is
`Copyright 2026 Hitesh Soliwal and contributors`; OpenJDK and third-party
notices remain separately scoped.

Additional exact public evidence identities:

| Evidence | Git blob SHA-1 | Raw-byte SHA-256 |
| --- | --- | --- |
| [m3/synexia-import/intakes/jni-array-address-receiver-20261007/SOURCE_PROVENANCE.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/jni-array-address-receiver-20261007/SOURCE_PROVENANCE.tsv) | `c0092407d4345cb5fc96d08f43b5e5d44fb277fb` | `be4c9976f1287ab98d5d5762e5c7522aa3f5142182a18bb4457f4d8c26782bff` |
| [m3/synexia-import/intakes/jni-array-address-receiver-20261007/TARGET_MAP.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/jni-array-address-receiver-20261007/TARGET_MAP.tsv) | `598628aefa0befcf28a743d54124c025c1a9fceb` | `4f78c5d2cc5719c2b49b355970ebfbcaf5deffb03d416bfcc599541e742044ca` |
| [m3/vendor/synexia/README.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/README.md) | `e9a7f0159de1e2c069662455202065a1c93fb522` | `95317a8e1f02ce87776d4b042781ffbbd3a8f38321ff7ed30e1891a51a315ca7` |
| [m3/vendor/synexia/NOTICE](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/NOTICE) | `cf6694a1316785ba2aa45394314bbae2e856c7fe` | `31cec4f2c4936b0e7a71a77847a3c20b0959e119a02ef5cf537f4f707fff9427` |
| [m3/vendor/synexia/synexia-indexstring/THIRD_PARTY_NOTICES.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/THIRD_PARTY_NOTICES.md) | `fa05a0e5785564d55e170a436f1be87d43c23287` | `8efe35d33754f86343cbb54320893934c3beb46d5f2cb9ab02c2584032f5f3ab` |
| [m3/docs/m3jdk21-porting-invariant.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/m3jdk21-porting-invariant.md) | `f65cc98e9088377755c51a447ce974356b28f15c` | `fa84270f09a42fe4dbeb245812e7b710297825e2bfd461bce99bb64056e1399d` |
| [m3/docs/synexia-apache2-borrowing-contract.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-apache2-borrowing-contract.md) | `4466d4af7299bb1875334fc78bd7fd903aca40bb` | `20d6460e3746913964d07712e7455b25c9f08ccba70cce49c15cd689bb54e9da` |
| [m3/build.sh](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/build.sh) | `5ac0b944e34bf5f226d77541adbcb28e35edcfe5` | `9119dec83045301a2166d0a1de3b1008dfd431eaf3b5317daa14dc7e41325807` |
| [m3/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/pom.xml) | `693156006961717eccfc649585e724d67682b3df` | `15f0d5a0e942749392480372ff4b414949becb11370d1dbe25dbdda7dfb50209` |
| [m3/synexia-import/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/pom.xml) | `6b3fa2948f055227790041553700f6abdef79eb5` | `d85d3d4ed6c2ed2b2c637d64805c5f9462e2157ba66019be15075ca1bafbddb0` |
| [m3/ports/indexstring/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/ports/indexstring/pom.xml) | `d3fb14b11cf2cf76185c02ea1ffac82167356ba6` | `d07cf94116968269d1367e6e4ad41c6bc847bb9410ade7ff542255a74d2f346b` |
| [m3/compatibility/synexia-reference/PROVENANCE.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/compatibility/synexia-reference/PROVENANCE.md) | `1a529a0ef11c29ccf055ab5f6bda5755a402c647` | `249a8f82e2b3cd32767d5ae6eb31569d59b1f4cd3cb264720b1b9473b3d325ce` |
| [AGENTS.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/AGENTS.md) | `2253be71930e69f34420a72df8e8ba278ae61fb5` | `7de3689f6941871be9af41171851efa87cea662b29d831ecf87eeea4a5a2919b` |
| [CONTRIBUTING.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/CONTRIBUTING.md) | `9fffe3af25ea1ca3f1fc13b35b922ddbad37502f` | `7051d015da6808e590211d2b0e33062f1e380efb09873fdba5d631509ce175eb` |
| [m3/compatibility/synexia-full-family-pin.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/compatibility/synexia-full-family-pin.tsv) | `3d24fc38677435453787ca67f3d5458dbfaf5b5f` | `5d2a89e133164bfc276ed834be47ad3d1bd2583e4bed08a7f94a7e1eb2b50bcc` |
| [m3/docs/synexia-full-family-catalogue.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-full-family-catalogue.json) | `8145c667c941a9358e9a5149489e455d950afc34` | `2e6fa85aeaaec5a6c37aa429b66d102ee9497735fdd5476ca4085d34667b5eef` |
| [m3/synexia-import/current-full-borrow/README.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/current-full-borrow/README.md) | `92347a83839d85d5e9b07b829a545f5c8a4c431a` | `9b6960f76ed3dfaf2cb97ad497b59d0c76294502d6d0047bed70a1dd7d5e26c6` |
| [m3/synexia-import/current-full-borrow/pin.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/current-full-borrow/pin.tsv) | `7b3c0b7d0a6b8daab40ef0062a9e9293d9b7abd2` | `d84e185891f08d76da765e43d1e138e33fbef99f50de9cc83a99c5284ab46ab5` |
| [m3/synexia-import/current-full-borrow/promotion-phases.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/current-full-borrow/promotion-phases.tsv) | `c4e8582f99caba32d718b2c1e86dae0854cdce3d` | `e4730068f321d551913a4cf7d40832f43b315c3973312c73b744ca5081758eb5` |
| [m3/vendor/synexia/third-party/mapbox-jni-ownership/LICENSE.txt](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/third-party/mapbox-jni-ownership/LICENSE.txt) | `b55eda305dda4632d55df7901dcf7278933c88b8` | `640f62b4b6e940645b96d12416e6db84ed191ebcfd10c3a0ccc9a1338f222726` |
| [m3/docs/shared-atom-concatenation.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/shared-atom-concatenation.md) | `4884a619e251b469ae4c651a6e9c595531df1776` | `823a000fb8189383d45ecdfacdf9d39bb92a416b9b9bacfeefd03a8f3312564d` |
| [doc/mindex-string-backing.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/doc/mindex-string-backing.md) | `dd968f4b756cf44e4ea41e2e55e8be690dfe1743` | `93b374a90d9a8195c1e2c2ac76b3bbfa1a7b5f3f7f158763ba0db7b5374dcf8e` |
| [m3/docs/name-mapping.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/name-mapping.json) | `288f20575d3f9d106571990f21944e7f46b0cf71` | `f99b80ededfb898804547ad3baf87cdc86aaccda4331c076591e2459d4f09ecb` |

Git blob SHA-1 uses Git object framing; raw-byte SHA-256 hashes only file bytes.
No hash result changes the custody-only, non-buildable, NOT_RUN or pending
qualification dispositions above. Broader repository/module/package coverage
remains with the guide inventory, not this eight-body completion boundary.

[agents]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/AGENTS.md
[array-access]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArray.java#L15-L20
[array-body]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArray.java#L7-L14
[array-equality]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArray.java#L21-L33
[borrowing]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-apache2-borrowing-contract.md
[bridge-pom]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/ports/indexstring/pom.xml
[bytes-copy]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedBytes.java#L18-L34
[bytes-slice]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedBytes.java#L11-L17
[catalogue]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-full-family-catalogue.json
[chars-buffer]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedChars.java#L36-L57
[chars-copy]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedChars.java#L22-L35
[chars-slice]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedChars.java#L13-L21
[cmake-arena]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/CMakeLists.txt#L52-L105
[cmake-jni]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/CMakeLists.txt#L2-L50
[compare-cursor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_string_compare.cpp#L55-L85
[compare-entry]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_string_compare.cpp#L88-L103
[compare-validation]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_string_compare.cpp#L6-L53
[contributing]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/CONTRIBUTING.md
[control-pom]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/pom.xml
[conversion-lifecycle]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayConversions.java#L61-L73
[conversion-ops]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayConversions.java#L33-L59
[conversion-state]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayConversions.java#L17-L31
[cpp-cache]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_arrays.cpp#L149-L187
[cpp-hash]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_arrays.cpp#L29-L72
[cpp-local]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_arrays.cpp#L9-L27
[cpp-meta]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_arrays.cpp#L305-L320
[cpp-metrics]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_arrays.cpp#L190-L238
[cpp-onload]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_arrays.cpp#L460-L472
[cpp-range-signature]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_arrays.cpp#L325-L330
[cpp-summary]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_arrays.cpp#L107-L147
[cpp-validation]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_arrays.cpp#L75-L105
[family-pin]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/compatibility/synexia-full-family-pin.tsv
[fixture]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/compatibility/synexia-reference/PROVENANCE.md
[foundation-build]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/build.sh
[handle-body]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/ArrayHandle.java#L8-L14
[import-pom]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/pom.xml
[name-map]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/name-mapping.json
[native-declarations]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayNative.java#L96-L109
[native-entry]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayNative.java#L13-L39
[native-export]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayNative.java#L51-L101
[native-full-copy]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_arrays.cpp#L240-L302
[native-metrics]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayNative.java#L40-L49
[native-range]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_arrays.cpp#L323-L459
[native-range-declaration]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayNative.java#L107-L109
[phases]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/current-full-borrow/promotion-phases.tsv
[policy]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/m3jdk21-porting-invariant.md
[pool-append]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L332-L389
[pool-bytes]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L209-L237
[pool-chars]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L238-L260
[pool-close]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L659-L682
[pool-conversion]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L280-L295
[pool-descriptor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L424-L456
[pool-fingerprint]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L390-L423
[pool-geometry]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L125-L187
[pool-get]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L296-L324
[pool-index]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L499-L560
[pool-join]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L261-L279
[pool-lock]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L188-L207
[pool-mapping]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L618-L658
[pool-open]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L32-L123
[pool-refresh]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L561-L617
[pool-signatures]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L458-L498
[pool-state]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L32-L73
[pool-stats]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L325-L331
[provenance]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/jni-array-address-receiver-20261007/SOURCE_PROVENANCE.tsv
[segment-hash]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedSegments.java#L239-L276
[segments-buffers]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedSegments.java#L169-L196
[segments-copy]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedSegments.java#L198-L238
[segments-cursor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedSegments.java#L40-L93
[segments-join]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedSegments.java#L94-L132
[segments-plan]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedSegments.java#L133-L167
[segments-state]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedSegments.java#L9-L39
[successor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/current-full-borrow/README.md
[successor-pin]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/current-full-borrow/pin.tsv
[target-map]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/jni-array-address-receiver-20261007/TARGET_MAP.tsv
