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
| [ArrayHandle.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/ArrayHandle.java#L1-L15) | 15 | 2: handles | `e128e1258ff2504f4a7c93212d9f54f1642c1fbc` | `78924fd83b14c4081ea269ed309a6683b2f4dcae7807098327392bbc2a73f2f7` |
| [SharedArray.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArray.java#L1-L34) | 34 | 2: identity; 5: output | `861b406a55d1ab17e1582dfe21c129dfffdecc1b` | `079830bc4ffa5a944d8fa2d505e5365226d5c2cba48e344994eb044470774189` |
| [SharedArrayConversions.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayConversions.java#L1-L74) | 74 | 6: result-handle LRU | `1b1005e1327619346e1fc0e33b9be3bed300448f` | `d29c1238f711bac432c458ee176579a715dea69de689656f42b4f8d2e2e4a3c9` |
| [SharedArrayNative.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayNative.java#L1-L110) | 110 | 7–8: JNI routing/export | `396dfbc0c6866e49c7f60df2a88418ef58e22431` | `6dd0dfcb7fb795f212b8a427af5665142d309674fcb7e9dc43bc5f5f9bf1d9c6` |
| [SharedArrayPool.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayPool.java#L1-L683) | 683 | 3–4: arena/admission; 6: conversion; 9: geometry/lifecycle | `d2eae7b477b8592a5c2f658104cb006bc9be0b52` | `2ec092cc7f465e9596dd6e2217893ecad4065077b43825320cc6f55adfc80875` |
| [SharedBytes.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedBytes.java#L1-L35) | 35 | 2: bytes; 5: slices/copies | `3b1402b6867ba981ddb2788addfdc9676fead6c9` | `aa4ad92bb335635aea2d750b4fda5e566fd7ae86b2a427b09f8f833895ed869c` |
| [SharedChars.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedChars.java#L1-L58) | 58 | 2: characters; 5: slices/copies | `34d1a73f043daed26c2181194cf7961d66bf9603` | `aa58df26d7c871c73a8837e37736f6b742a19b276fc15404ba2b1d9c2ad05bc0` |
| [SharedSegments.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedSegments.java#L1-L277) | 277 | 4–5: geometry/cursors; 8: range plans | `48aab5883a38394d8d3fc564761e2444e54d24c9` | `400af366a2f39e017aedfa97d67d56383bb19a21da453483ea821d53c903d81c` |

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
| [shared_arrays.cpp](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/native/shared-arrays/shared_arrays.cpp) | 472 | `182520825505422c2d7b5570cb3b2bb556b0f222` | `b921df8c71aadd34f79f1e79bca634a13145c158fee0ab1299ca23cb692730ae` |
| [shared_string_compare.cpp](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/native/shared-arrays/shared_string_compare.cpp) | 103 | `c3fc9b5f58ca4bee925f17578fd499ae886c75c0` | `e11dd837662dbd18bdae30f6b48a9827124d8e1e036d6ffa66a2f9e3c98623c8` |
| [CMakeLists.txt](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/native/shared-arrays/CMakeLists.txt) | 105 | `ddb1f77a6f520a266d62258f5d3277e28f533a10` | `60631ff0d3338e78d45e2c19ce4274191383dfd88cc2cf221ae425a9fe93f4a8` |

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
`f78685ca6efb5191e4e11b0bcfd137ddac7430b9` and
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
| [m3/m3-import/intakes/jni-array-address-receiver-20261007/SOURCE_PROVENANCE.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/jni-array-address-receiver-20261007/SOURCE_PROVENANCE.tsv) | `0840129088a61019ddf00ff698977f5e7a3d0df4` | `e5a65d94e31a48afb7d5a67cf359ff510f2fe943665d94ed73fced7fd47117d2` |
| [m3/m3-import/intakes/jni-array-address-receiver-20261007/TARGET_MAP.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/jni-array-address-receiver-20261007/TARGET_MAP.tsv) | `f14ae1e0297a1dc7a61871435a10c48256dfb20d` | `4dca3875492eeb79a40acc0ce17edeb964b5bdd408c508b1473140e835b49af9` |
| [m3/vendor/m3/README.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/README.md) | `655eaa7247076b2e9e06401420d8b9fe3af8aa0f` | `b4ac6e882040df220d3d9d8682665ad59d47e17fc69f29bf169e4782e1e2792a` |
| [m3/vendor/m3/NOTICE](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/NOTICE) | `cf6694a1316785ba2aa45394314bbae2e856c7fe` | `31cec4f2c4936b0e7a71a77847a3c20b0959e119a02ef5cf537f4f707fff9427` |
| [m3/vendor/m3/m3-indexstring/THIRD_PARTY_NOTICES.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/THIRD_PARTY_NOTICES.md) | `fa05a0e5785564d55e170a436f1be87d43c23287` | `8efe35d33754f86343cbb54320893934c3beb46d5f2cb9ab02c2584032f5f3ab` |
| [m3/docs/m3jdk21-porting-invariant.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/m3jdk21-porting-invariant.md) | `d933c7f8a4ba875b318c499214857146724fc4da` | `604ef82225fdb9e6ecf00e14f7630fb0956b85ec89e5c9241537733d4054f39a` |
| [m3/docs/m3-apache2-borrowing-contract.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/m3-apache2-borrowing-contract.md) | `63b9ea3d38b9413b6b574a875aad5a63c49f91e4` | `a4bbdfa9e272b73ff1b02ff6b9159c978f01a359242424d83c7a3a5ccd08a6ec` |
| [m3/build.sh](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/build.sh) | `be764d854ce1d7686fd53da8953110a382ec1dec` | `c7b954a6faa16cc8a0cb4a2d13a8032c43fb308a739b225362b6ac3fd84864ee` |
| [m3/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/pom.xml) | `e01cddcbac3751bfa79368c3727343754c7e3b21` | `37b503fb5aee9a1f3a02d0efaa8b224f31c9c2aaa5d8e0079143bc9bd8fb73fb` |
| [m3/m3-import/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/pom.xml) | `6b3fa2948f055227790041553700f6abdef79eb5` | `d85d3d4ed6c2ed2b2c637d64805c5f9462e2157ba66019be15075ca1bafbddb0` |
| [m3/ports/indexstring/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/ports/indexstring/pom.xml) | `d3fb14b11cf2cf76185c02ea1ffac82167356ba6` | `d07cf94116968269d1367e6e4ad41c6bc847bb9410ade7ff542255a74d2f346b` |
| [m3/compatibility/m3-reference/PROVENANCE.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/compatibility/m3-reference/PROVENANCE.md) | `400e30b64110faa62d872332093042549da8318d` | `7f5683751c72db91dac37ae83cb97e02882ad773040a88d47e9ad466ba303d55` |
| [AGENTS.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/AGENTS.md) | `f78685ca6efb5191e4e11b0bcfd137ddac7430b9` | `d9cc483da6c7330b30a454f6ce65868978167e302bcdb0805bf6d8a5da018621` |
| [CONTRIBUTING.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/CONTRIBUTING.md) | `9fffe3af25ea1ca3f1fc13b35b922ddbad37502f` | `7051d015da6808e590211d2b0e33062f1e380efb09873fdba5d631509ce175eb` |
| [m3/compatibility/m3-full-family-pin.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/compatibility/m3-full-family-pin.tsv) | `3d24fc38677435453787ca67f3d5458dbfaf5b5f` | `5d2a89e133164bfc276ed834be47ad3d1bd2583e4bed08a7f94a7e1eb2b50bcc` |
| [m3/docs/m3-full-family-catalogue.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/m3-full-family-catalogue.json) | `8145c667c941a9358e9a5149489e455d950afc34` | `2e6fa85aeaaec5a6c37aa429b66d102ee9497735fdd5476ca4085d34667b5eef` |
| [m3/m3-import/current-full-borrow/README.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/current-full-borrow/README.md) | `11fbf03808fe5d1999b38766eebd7d1944ace049` | `8a010b39ee657098078ddc8e9502a545b6628c9b23c0b3f63526423ba36a4b55` |
| [m3/m3-import/current-full-borrow/pin.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/current-full-borrow/pin.tsv) | `8bdaaaf8bacbaf190e537e82955915a720592c92` | `8cdf72c68a0075ad8b3bb0968369619d1c4c34bc675d785d19aec39717f495b4` |
| [m3/m3-import/current-full-borrow/promotion-phases.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/current-full-borrow/promotion-phases.tsv) | `c4e8582f99caba32d718b2c1e86dae0854cdce3d` | `e4730068f321d551913a4cf7d40832f43b315c3973312c73b744ca5081758eb5` |
| [m3/vendor/m3/third-party/mapbox-jni-ownership/LICENSE.txt](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/third-party/mapbox-jni-ownership/LICENSE.txt) | `b55eda305dda4632d55df7901dcf7278933c88b8` | `640f62b4b6e940645b96d12416e6db84ed191ebcfd10c3a0ccc9a1338f222726` |
| [m3/docs/shared-atom-concatenation.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/shared-atom-concatenation.md) | `4884a619e251b469ae4c651a6e9c595531df1776` | `823a000fb8189383d45ecdfacdf9d39bb92a416b9b9bacfeefd03a8f3312564d` |
| [doc/mindex-string-backing.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/doc/mindex-string-backing.md) | `69d890f969a8e67d69d52b104a2f3ed4b4418f5e` | `d45d09126f163d5915fbe54e209665f6d2b9550c6eaadb30d70f0751b387650e` |
| [m3/docs/name-mapping.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/name-mapping.json) | `288f20575d3f9d106571990f21944e7f46b0cf71` | `f99b80ededfb898804547ad3baf87cdc86aaccda4331c076591e2459d4f09ecb` |

Git blob SHA-1 uses Git object framing; raw-byte SHA-256 hashes only file bytes.
No hash result changes the custody-only, non-buildable, NOT_RUN or pending
qualification dispositions above. Broader repository/module/package coverage
remains with the guide inventory, not this eight-body completion boundary.

[agents]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/AGENTS.md
[array-access]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArray.java#L15-L20
[array-body]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArray.java#L7-L14
[array-equality]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArray.java#L21-L33
[borrowing]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/m3-apache2-borrowing-contract.md
[bridge-pom]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/ports/indexstring/pom.xml
[bytes-copy]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedBytes.java#L18-L34
[bytes-slice]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedBytes.java#L11-L17
[catalogue]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/m3-full-family-catalogue.json
[chars-buffer]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedChars.java#L36-L57
[chars-copy]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedChars.java#L22-L35
[chars-slice]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedChars.java#L13-L21
[cmake-arena]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/native/shared-arrays/CMakeLists.txt#L52-L105
[cmake-jni]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/native/shared-arrays/CMakeLists.txt#L2-L50
[compare-cursor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/native/shared-arrays/shared_string_compare.cpp#L55-L85
[compare-entry]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/native/shared-arrays/shared_string_compare.cpp#L88-L103
[compare-validation]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/native/shared-arrays/shared_string_compare.cpp#L6-L53
[contributing]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/CONTRIBUTING.md
[control-pom]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/pom.xml
[conversion-lifecycle]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayConversions.java#L61-L73
[conversion-ops]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayConversions.java#L33-L59
[conversion-state]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayConversions.java#L17-L31
[cpp-cache]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/native/shared-arrays/shared_arrays.cpp#L149-L187
[cpp-hash]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/native/shared-arrays/shared_arrays.cpp#L29-L72
[cpp-local]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/native/shared-arrays/shared_arrays.cpp#L9-L27
[cpp-meta]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/native/shared-arrays/shared_arrays.cpp#L305-L320
[cpp-metrics]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/native/shared-arrays/shared_arrays.cpp#L190-L238
[cpp-onload]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/native/shared-arrays/shared_arrays.cpp#L460-L472
[cpp-range-signature]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/native/shared-arrays/shared_arrays.cpp#L325-L330
[cpp-summary]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/native/shared-arrays/shared_arrays.cpp#L107-L147
[cpp-validation]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/native/shared-arrays/shared_arrays.cpp#L75-L105
[family-pin]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/compatibility/m3-full-family-pin.tsv
[fixture]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/compatibility/m3-reference/PROVENANCE.md
[foundation-build]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/build.sh
[handle-body]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/ArrayHandle.java#L8-L14
[import-pom]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/pom.xml
[name-map]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/name-mapping.json
[native-declarations]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayNative.java#L96-L109
[native-entry]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayNative.java#L13-L39
[native-export]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayNative.java#L51-L101
[native-full-copy]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/native/shared-arrays/shared_arrays.cpp#L240-L302
[native-metrics]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayNative.java#L40-L49
[native-range]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/native/shared-arrays/shared_arrays.cpp#L323-L459
[native-range-declaration]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayNative.java#L107-L109
[phases]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/current-full-borrow/promotion-phases.tsv
[policy]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/m3jdk21-porting-invariant.md
[pool-append]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayPool.java#L332-L389
[pool-bytes]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayPool.java#L209-L237
[pool-chars]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayPool.java#L238-L260
[pool-close]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayPool.java#L659-L682
[pool-conversion]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayPool.java#L280-L295
[pool-descriptor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayPool.java#L424-L456
[pool-fingerprint]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayPool.java#L390-L423
[pool-geometry]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayPool.java#L125-L187
[pool-get]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayPool.java#L296-L324
[pool-index]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayPool.java#L499-L560
[pool-join]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayPool.java#L261-L279
[pool-lock]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayPool.java#L188-L207
[pool-mapping]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayPool.java#L618-L658
[pool-open]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayPool.java#L32-L123
[pool-refresh]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayPool.java#L561-L617
[pool-signatures]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayPool.java#L458-L498
[pool-state]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayPool.java#L32-L73
[pool-stats]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedArrayPool.java#L325-L331
[provenance]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/jni-array-address-receiver-20261007/SOURCE_PROVENANCE.tsv
[segment-hash]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedSegments.java#L239-L276
[segments-buffers]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedSegments.java#L169-L196
[segments-copy]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedSegments.java#L198-L238
[segments-cursor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedSegments.java#L40-L93
[segments-join]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedSegments.java#L94-L132
[segments-plan]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedSegments.java#L133-L167
[segments-state]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/shared/SharedSegments.java#L9-L39
[successor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/current-full-borrow/README.md
[successor-pin]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/current-full-borrow/pin.tsv
[target-map]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/jni-array-address-receiver-20261007/TARGET_MAP.tsv
