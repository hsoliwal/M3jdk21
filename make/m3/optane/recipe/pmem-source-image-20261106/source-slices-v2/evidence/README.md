<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Source-slices v2 execution receipt

Executed on 2026-11-01 against all 45 exact module blobs from
`hsoliwal/com.synexia@f75ddfc10fc161af646dea9660a650545ded7661` (existing PR 15204).
`baseline-inputs.tsv` records their Git blob and SHA-256 identities.
`qualified-inputs.tsv` binds the tested source, build, recipe and template bytes.
The existing source-slices manifest seals all eight generated postimages; the
versioned baseline manifest and seven retained preimages admit the upgrade.
The new JUnit source has explicit absent-before admission.
Archived tool output removes trailing spaces and excess terminal blank lines only;
substantive output and observed results are retained.

## Observed results

| Gate | Result | Receipt |
|---|---|---|
| Original Java21 / JNI / JMOD / jlink baseline | Passed before changes; original sparse/sampled source test only | `baseline-build.log` |
| Empty-source closed-mapping regression | Failed on original implementation; passed after fix | `empty-closed-red.log`, `empty-closed-green.log`, `EmptySliceClosedRegression.java` |
| Caller-buffer API regression | Original API rejected all 16 overload call sites at compilation | `buffer-api-red.log` |
| Original create-only recipe | Refused an exact existing-source upgrade | `recipe-upgrade-red.log` |
| Permission-preserving recipe regression | Initial atomic replacement changed 0755 to 0600; final upgrade retains script 0755 and POM 0640 | `recipe-permissions-red.log`, final Maven recipe output |
| Maven verify, Java21, POSIX JNI | BUILD SUCCESS; 10 real JUnit tests, no failures/errors/skips | `maven-verify.log`, `junit-maven-summary.txt` |
| Retained smoke contracts | 23 SDK + 25 transfer + 22 source-slice assertions, on ordinary JVM and linked runtime | `maven-verify.log` |
| Complete source-image traversal | Each of two Maven-run traversals verified 20,000 entries and 1,000,000,000 bytes with one reusable 50,000-byte buffer | `maven-verify.log` |
| Interpreted JVM / checked JNI | Same 10 JUnit tests passed under `-Xint -Xcheck:jni` | `junit-interpreted.log` |
| C17 / undefined-behavior sanitizer | Same 10 real JNI JUnit tests passed with UBSan halt-on-error | `junit-ubsan.log` |
| Bounded Java heap | Complete 20,000-entry / 1GB traversal passed under `-Xmx32m -Xcheck:jni` | `native-32m-heap.log` |
| API and native exports | Every prior public member retained; exactly one additive `readSealed(int, byte[], int)` overload; all 11 JNI exports identical | `public-api-{before,after}.txt`, `jni-symbols-{before,after}.txt` |
| Recipe admission | Eight absent targets created; exact seven-existing/one-absent upgrade; repeat fixed point; unknown source/template/baseline drift, late-target drift and symlinked target ancestor rejected | `maven-verify.log` |

The scale fixture contains 20,000 distinct synthetic ASCII source entries packed
into one file, fully written and synced before reading. The compatibility-array
check and mutation-refusal probe are additional operations outside the counted
20,000-entry traversal. This is neither a real repository quality campaign nor
an inference benchmark. All tests execute **zero model calls**.

Traversal timings cover the read/hash loop after fixture creation. They exclude
source gathering, image creation, parser/quality work and model inference. The
recently written image can be served by the OS page cache; these measurements
do not establish cold-disk throughput or permanent physical residency. The
32 MiB heap cap excludes native mappings, OS file cache and other process memory.
The new overload avoids a fresh file-sized Java array per read; digest state and
the 32-byte digest result still allocate per invocation.

## Reproduction

Primary module qualification, from the repository root, with Java 21 and Maven
on PATH and the already available tool dependencies:

```sh
M3_USE_PMDK=off mvn -o -B -ntp -f m3-pmem-sdk/pom.xml verify
java -Xmx32m -Xcheck:jni -Djava.library.path=m3-pmem-sdk/target/native \
  --module-path m3-pmem-sdk/target/classes --add-modules com.synexia.m3.pmem \
  -cp m3-pmem-sdk/target/test-classes test.PmemSourceSlicesNativeSmokeTest
```

The recorded run used Maven 3.9.11, Temurin 21.0.13+9, GCC 13.3.0, Linux amd64,
JUnit Jupiter 5.10.2, compiler-plugin 3.13.0 and Surefire 3.2.5. Toolchain output
is retained in `toolchain.txt`. Maven used an isolated copied local dependency
cache via `-Dmaven.repo.local`; no network resolution was used by the run.

For the supplemental JUnit invocations, compile `JUnitQualification.java` with
the same production and JUnit test classes and the following test-tool JARs:
JUnit Platform launcher/engine/commons 1.10.2, Jupiter engine/API 5.10.2,
OpenTest4J 1.3.0 and API Guardian 1.1.2. This launcher selects the checked-in
`test.PmemSourceSlicesJUnitTest` and requires exactly ten successful tests.
Run it once with `-Xint -Xcheck:jni` against `target/native` and once with
`-Xcheck:jni` against a separately compiled sanitized library:

```sh
cc -std=c17 -O1 -g -Wall -Wextra -Werror -fPIC -shared \
  -fsanitize=undefined -fno-sanitize-recover=undefined \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
  m3-pmem-sdk/src/main/c/m3pmemsdk.c -o "$SANITIZED_DIR/libm3pmemsdk.so"
UBSAN_OPTIONS=halt_on_error=1:print_stacktrace=1 \
  java -Xcheck:jni -Djava.library.path="$SANITIZED_DIR" \
  -cp "$QUALIFICATION_CLASSPATH" JUnitQualification
```

`SANITIZED_DIR` is a created temporary output directory and
`QUALIFICATION_CLASSPATH` contains the compiled helper, production/test classes
and the listed test-tool JARs. The sanitized library does not replace the ordinary
JNI/JMOD artifact. The module's ordinary native build remains unchanged.

## Independent review and dispositions

A separate read-only reviewer checked the entire tracked diff and new files,
including all eight postimage seals, all seven original preimages, baseline
absence of JUnit, recipe baseline seal, evidence logs and API/JNI differences.
The reviewer found two minor issues: atomic replacement changed POSIX file
permissions, and the first edited postimage manifest lost its SPDX lines. Both
were corrected; the final Maven run includes the permission regression and the
manifest retains its original attribution. No unresolved code findings remained.

The following are explicit boundaries of this module qualification. They are
retained as integration or receiving-platform gates, not silently accepted:

| Boundary | Exact inspected owner / reason | Required next action |
|---|---|---|
| Repository/path/revision and immutable backing-file lease | Existing M3File/source host; a read-only mapping cannot prevent external truncation | Bind canonical file identities and hashes to slices; retain the snapshot lease through dispatch; prove mutation/truncation exclusion in the host |
| PMEM-to-quality handoff | Source admission PR 15187 at `29f6c5ab7dc3df2c28c164134a554487336755fe` accepts filesystem paths, without a PMEM-byte overload | Connect strict UTF-8 decoding and lazy source reads to the existing source/admission owner; validate exact per-file quality joins |
| Durable one-attempt inference | Quality campaign PR 15169 at `f59da4ec7eb5e8a381b282aceebb06f8e6a51afe` exposes claim/staging callbacks, which do not establish a durable backing store | Exercise the real host with failure/restart receipts and one generation-attempt identity per file; complete an actual Chrome campaign separately |
| 50,000-byte model payload | Existing Chrome proposal owner caps full source at 48 KiB (49,152 bytes) | Use its verified dossier path or separately admit a packet-budget change; retain model token admission and never silently truncate |
| Physical residency and throughput | This test proves mapped-byte traversal and a bounded Java heap only | Measure native/process memory and cold/warm workload behavior on the target machine before a residency or throughput claim |
| Windows, libpmem2/Optane and M3JDK21 receiving runtime | No such platform/hardware execution in this Linux POSIX module snapshot | Run the existing receiving runtime and backend-specific test owners before platform/product promotion |
| Full Synexia reactor, source/dependency provenance and CI | Only this exact 45-file module baseline plus recipe output was built locally | Execute the canonical repository admission/reactor and existing bounded CI lane on the receiving revision; these module receipts do not replace them |
| Concurrent recipe writers / global transaction | Exclusive source-image ownership is required; replacement is atomic per file | Keep one writer; resume admitted partial upgrades through the existing recipe; do not infer multi-file transaction semantics |

The existing category/donor records were inspected. The operation remains a
bounded indexed read plus SHA-256 verification; interval merging, prefix sums,
new hash indexes and cache hierarchies add no required behavior. Existing
PmemRegion, JDK MessageDigest and exact-source recipe owners were reused.
No challenge solution, external donor source, new runtime dependency, scheduler
or alternative storage owner was copied into this change.

These are executed **Java exact-source Maven recipe** receipts. They are not
OpenRewrite SDK execution, full-repository acceptance, hardware persistence
qualification or live Chrome inference evidence.
