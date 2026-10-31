<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3FS capability port qualification — 2026-10-19

The delivery direction is **pinned MIndexFS capability source → M3FS owned by
M3JDK21 → Eclipse Resources consumption through EFS**. The generated receiver
contains the Java/native implementation, a `jdk.m3fs` modular artifact, runtime
packaging through the existing M3JDK21 JMOD tools, and an original EFS provider.
The reusable transfer recipe remains in Synexia. No Synexia runtime package or
service is required by M3FS.

This report qualifies the reconstructed October 19 sources. An earlier scratch
workspace was lost before implementation commits or PRs existed; October 18
build outputs are not used to qualify these bytes.

## Source custody and representation

The donor is `hsoliwal/com.synexia@289e32f130c9c8e4a1cf0edc1066327d699393bf`.
The receiving preimage is
`hsoliwal/M3jdk21@2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.
The Eclipse contracts were inspected at
`hsoliwal/eclipse.platform@2bfb19847bd09bf28749194d489e8696b4a997b1`.

[DONORS.tsv](DONORS.tsv) accounts for 101 exact inputs: 69 Java sources and
fixtures, 25 native sources/build inputs, six Eclipse references, and the donor
license. The inventory records Git blob identity, SHA-256, byte count, source
revision, license and disposition. All six Eclipse rows are reference-only;
no Eclipse core implementation source is embedded. [ADAPTERS.json](ADAPTERS.json)
maps all 69 Java inputs through 59 adaptation groups, including the selected
cold-format and regex/text helper atoms. It does not claim that unrelated
compiler or String-engine source estates have been ported.

Directory metadata uses packed character storage plus offset/state/scalar arrays.
Generations, manifests, precompute metadata and mapped references retain parallel
name/scalar columns and packed SHA-256 bytes. A generation's N path identities
occupy a `byte[]` of 32 × N bytes beside its normalized `String[]` paths. Internal
lookup, decoding, hashing and updates use these columns. Immutable public maps,
lists, rows, content IDs and references are projected when the API requests them.
Native handles, mapped buffers and explicit resource lifetime owners remain
objects; the representation does not claim allocation-free execution.

Canonical-byte fixtures preserve the donor generation, precompute-key, manifest,
bundle and pipeline formats. The sealed 12,460-byte catalogue compatibility
fixture retains its exact original SHA-256. SQLite schema 1, native ABI 1 and
historical image encodings remain explicit compatibility contracts.

## Executed qualification

| Gate | Fresh result and evidence |
| --- | --- |
| Generated receiver backend Maven `install` | **97 Java tests in 31 nonempty suites; zero failures, errors or skips.** Native loading is required and Java runs with `-Xcheck:jni`. [Result](evidence/backend/generated-receiver-result.json), [build log](evidence/backend/generated-receiver-maven-install.log). |
| Generated receiver native CTest | **12/12 passed**, in the same Maven build. |
| Native sanitizers | **12/12 passed with ASan, UBSan and LSan.** [Accepted run](evidence/native/sanitizers-ctest.log), [reproduction](evidence/native/reproduce.txt). |
| JNI source/header/binary parity | Java 21 compiled all **33 production Java sources** with `-Xlint:all -Werror` and generated two native headers. **45 generated JNI functions exactly match 45 exports**, with zero missing or unexpected exports. Both JNI C translation units compile with generated declarations force-included. [Receipt](evidence/native/jni-abi.json). |
| Recipe behavior and admission | **Five JUnit tests passed** for permutations/fixed points, opaque source preservation, absent/occupied/drifted/duplicate targets, unsafe names/template drift, all-path preflight and symbolic-link refusal. The actual receiver applied the initial 163-path packet, then check/reapply reported zero changes with an identical ordered content root. [Initial application](evidence/recipe-initial-application.json). Final replay and manifest binding are retained by the canonical crate. |
| Generated receiver linked runtime | **Passed** using the actual receiver wrapper and existing JMOD packer. The probe resolves M3FS through `jrt:/jdk.m3fs`, loads JNI and SQLite from the image, and exercises direct I/O, retained snapshot/live distinction, clone isolation, scrub, checkpoint and read-only reopen. [Proof](evidence/runtime/generated-receiver-proof.json), [inputs](evidence/runtime/generated-receiver-inputs.json). |
| Generated receiver Eclipse, backend OSGi bundle | **14 provider tests plus one real Equinox test, with all 15 named workspace checks; zero failures, errors or skips.** [Receipt](evidence/eclipse/generated-receiver-normal.json), [tests](evidence/eclipse/generated-receiver-normal-tests.txt). |
| Generated receiver Eclipse, linked `jdk.m3fs` | **14 provider tests plus one real Equinox test, with all 15 named workspace checks; zero failures, errors or skips.** The test runs on the new receiver image, checks module/class origin, and supplies no native-path override. [Receipt](evidence/eclipse/generated-receiver-linked.json), [tests](evidence/eclipse/generated-receiver-linked-tests.txt). |
| Existing receiving-phase invariant | **Passed for all 32 families**, retaining `SOURCE_PIN_ONLY`, ordered String → arrays → collections → AST/compiler → remaining families, and `completion=false`. [Receiver check](evidence/full-borrow-generated-receiver.log). |
| Packaged legal files | **All five files match their sources**, including the exact CRLF Apache license, notices and the supplied SQLite distribution copyright. [Legal receipt](evidence/runtime/generated-receiver-legal.json). |

The Equinox test is one JUnit test containing 15 named workspace assertions;
those assertions are not counted as 15 additional JUnit tests. Staging and
generated receiver runs are preserved separately. The generated receiver JAR
and JNI library are byte-identical to the qualified staging artifacts, so the
earlier sanitizer and JNI ABI receipts bind the same production bytes.

## Artifact identity

| Artifact | SHA-256 |
| --- | --- |
| Generated `m3-fs-0.1.0-SNAPSHOT.jar` | `d26696e0b79b81cbc51d8c07e0e19c574111e6603b930c15006a01c8d8c69970` |
| Generated input `libm3fs.so` | `fb2e70989a90f1ff151519ad875c09f82a3812ec7e2d7cfc932c2a001eccfcdf` |
| Linked image `libm3fs.so` | `856c1d9bb8ea084970f73b811c3a7a91f2d89921538080ef01e7dfad0111a7a6` |
| Linked image SQLite | `2e6eef9a727f081f0d453b4e5e6cbd8b9ef8b6f86cbf7681cbad444d3b0b55c8` |

The Eclipse provider and scenario archives are identical between the generated
receiver normal and linked runs: provider SHA-256
`299588bb2b8d7358f8886ba8d4ffe2d4ca7ccd79b08126ca57300ef9389a4923`, scenario
`c3f63cf5ceabf9286b6b0f4af6f419f8cdf407e873c2a29d78f3750fc43e5803`. Their
decoded entry names and bytes match staging; ZIP Unix permission metadata differs,
so whole-archive equality with staging is not claimed. The Eclipse receipts
record the actual archive hashes and the entry-content comparison.

The existing JMOD packer uses `jlink --strip-debug`, so input and linked JNI
hashes are intentionally recorded separately. Runtime proofs use the linked
hash and actual loaded image path. No backend JAR is supplied on the linked
probe's classpath. Loader/Java injection variables are cleared for image checks.

## Bounded resource observations

The [resource report](evidence/resource/RESOURCE_QUALIFICATION.md) compares the
untouched donor and receiver on one deterministic 50,000-row namespace in
separate Temurin 21 JVMs, with three warmups and three measurements each.

| Observed median | Donor | Receiver |
| --- | ---: | ---: |
| Current-thread construction CPU | 178.364 ms | 85.615 ms |
| Construction allocation | 51,653,824 bytes | 48,653,664 bytes |
| Incremental post-GC owner heap | 9,395,520 bytes | 9,195,432 bytes |

All six measured roots produced identical 3,451,572-byte canonical encodings,
SHA-256 `9ba5076f50d1643150a4ab24c9fea08d10cecb448850edce4facafb4383a694f`.
The [raw receipt](evidence/resource/resource-measurements.json) preserves ranges,
each sample, source/class/toolchain hashes, process RSS, child-specific `wait4`
CPU/peak RSS and exit NMT totals. The prepared public input map remains live in
both processes. Shared-host scheduling, JIT/GC activity, baseline-heap variation
and the small sample count limit interpretation. These are construction
observations; native filesystem I/O, API enumeration and Eclipse workloads are
outside the probe. NMT covers the whole JVM and does not isolate M3FS native
allocation. No general throughput, memory-improvement or steady-state claim is
made.

## Reproduction and corrections

Use Java 21, Maven, CMake, a native compiler and SQLite development files. From
the generated receiver, the main commands are:

```bash
mvn -B -ntp -f m3/fs/pom.xml install
mvn -B -ntp -f m3/eclipse/filesystem/pom.xml \
  -Dm3.fs.native.path="$PWD/m3/fs/target/native/libm3fs.so" clean verify
python3 m3/compatibility/check_synexia_full_borrow_receiver.py
```

The module [README](../../README.md), Eclipse
[README](../../../eclipse/filesystem/README.md), and runtime receipts document
custom toolchain paths and linked-mode commands. This environment used Temurin
21.0.12.1+1, Maven 3.9.11, GCC 13.3.0, CMake 3.25.1 and native SQLite 3.40.1.
The final backend/recipe/Eclipse Maven runs used the restored pinned local cache
offline. JNI compilation requires the configured JDK headers; the optional
FUSE projection requires FUSE3 development files.

[FAILURES_AND_CORRECTIONS.md](FAILURES_AND_CORRECTIONS.md) records observed
failures and their successful reruns, including native-suite assumptions, the
restored sealed fixture, a concurrent directory-creation race and the recipe
CLI logging dependency. A final documentation-only correction reconciled the
remaining-gates paragraph with the completed bounded resource probe. It was
applied by the unchanged hash-pinned recipe engine from an exact one-file
preimage, then replayed with zero changes; its [receipt](evidence/recipe-resource-evidence-correction.log)
records both hashes. No executable source changed after receiver qualification.

## Gates remaining before wider promotion

This draft does not claim a complete modified OpenJDK/M3JDK21 or HotSpot build;
jtreg, C1/C2, GC or CDS qualification; the entire Eclipse fork's Tycho suite or
GUI workbench; Windows/macOS runtime qualification; a mounted FUSE filesystem;
power-loss fault injection; or broad performance/GC-latency acceptance. FUSE
was compiled only. The linked image is a stock Java 21 image containing the new
M3FS module, not a qualification of the entire modified JDK.

The precompute port stores and consumes supplied segment, transition, output,
n-gram and related lanes. Catalogue profile/pipeline names describe supported
persisted shapes and mapped consumption; they do not implement every named
execution backend. Native regex consumes capture-free compiled images, reports
unsupported values explicitly, and uses exact-image-bound JDK or caller-supplied
fallback where configured. The target compiler/AST-to-image producer bridge and
GPU/accelerator engines remain separate work. The repository's wider ordered
source-estate receiving phases and completion flags are unchanged.
