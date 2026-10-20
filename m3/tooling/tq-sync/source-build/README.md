# Source-built producers and TQ fixture qualification

SPDX-License-Identifier: Apache-2.0

This tooling reuses the Maven recipe in `../source-reactor/pom.xml`, the existing vendored producer POMs, the guarded source-hash helper, the existing parameterized OpenRewrite text recipe and the bounded hosted execution lane.

The first aggregate install at public `0349d8c439868803e87a22885b148a6339483023` stopped under the storage cap while compiling producer 46 of 57. No complete producer or TQ JUnit qualification resulted. Its outer job log is retained in the canonical source task; the complete producer log and artifact bundle were unavailable. This revision addresses that measured output problem without increasing the cap.

Execution uses the following dependent steps:

1. Run the six existing real Maven model cases, including negative and stale-closure cases, against the current pinned POMs.
2. Expand the exact source checkout under the monitor; verify 8,513 provenance files, 342 metadata files, all collected POMs, the shared engines and the TQ source tree.
3. Ask Maven itself for the exact 57-producer order using the existing five selectors and `-am validate`. The earlier model's 57 includes 56 producers plus TQ and is a different vector.
4. Install each unchanged producer POM in a separate Maven session in that native order. Compare actual target and installed JAR bytes, classes and coordinates, and actual emitted versus installed POM bytes. Preserve the three native Shade reduced POMs as bounded XML evidence.
5. Invoke native Clean 3.5.0 for that module only. Require target absence and unchanged installed artifacts. Keep the isolated Maven repository and dependency caches. Target equality is an at-install-time witness; final checks rehash retained installed artifacts after targets are cleaned.
6. Run eight artifact guard tests using isolated copies of genuine produced artifacts and completed receipts. Copies remain in bounded `ROOT/tmp/NOT_PRODUCER-*` directories until ephemeral job teardown. Originals are rehashed afterward. Failed-native-command cases test receipt/status admission; they do not claim deliberately failing Maven executions.
7. Run the existing TQ Maven recipe with consumer tests enabled. Require its three unchanged JUnit methods and 24 exact generated postimages, plus three named fixture-manifest recipe tests and one exact generated manifest. Revalidate all installed producer artifacts afterward.

The producer selectors remain `rewrite-java-21`, `rewrite-test`, `slf4j-api-2.0.9`, `junit-jupiter-5.10.2` and `junit-platform-launcher-1.10.2`. Source-qualified coordinates and producer POM test policies remain intact. Real installed artifacts are required; historical PASS rows are not admission.

Current receiver reconciliation preserves nine metadata/verifier corrections and API Guardian's existing-license packaging fix. Source payloads and coordinates remain unchanged. The fixture repair changes exactly four stale AFTER hashes to match already-current resources. Its named forward/inverse descriptors reuse `M3Jdk21HashPinnedTextSnapshotRecipe`; exact replay, fixed point, refusal, unrelated input and rollback are checked in the owning Maven module. Every old BEFORE hash, all 24 paths, the resource bodies and current live owner/mapping gates remain intact.

The consumer command remains `source-reactor/pom.xml -Dm3.receiver.relative=../../../.. -pl com.m3:tq-sync -DskipTests=false -Dmaven.test.skip=false verify`. Its two required suites are `TQSyncTest` and `TQFixtureManifestConsistencyTest`, each with three named non-skipped methods. The named repair output must equal the committed current fixture manifest.

Storage controls remain 512 MiB aggregate and 8 GiB free after planned output. Existing JDK 21, exact Maven 3.10.0 and CPython 3.12 are selected without a setup/download fallback. The census includes Git, checkout, Maven repository/cache, targets, temporary files, logs and artifact payloads. It retries a complete census at most three times only for admitted native-clean target removal; source/M2 directory and other I/O errors fail. Supervisor boundary tests exercise the same gate with mocks and do not grant producer admission.

Command logs and new receipts are written directly into the artifact directory before bulk work. Existing model evidence is retained before source compilation. A separate read-only upload fence enforces a flat regular-file payload of at most 64 files and 8 MiB on all outcomes. Full bounded failed logs can therefore survive a final storage stop without another large copy. JARs and generated source payloads remain in-job evidence; their hashes and reports are exported.

Qualification imports only guarded `verify_crate.sha256(Path)` and the existing model helpers. It does not execute the complete 131-row verifier. No new dependency resolver, compiler or recipe engine is introduced. See the canonical source task's `SERIAL_DONORS.md` for pinned Apache Maven/Clean/Shade reuse and notices.

A passing job qualifies source producer build/installation, guard tests, TQ fixture JUnit and the exact fixture-manifest transformation only. Fresh donor API equivalence, full upstream suites, rebuilt native libraries, current product custody, the separate runtime TQ/JNI harness, canonical overflow/original private model, complete matched JDK and performance/retention remain separate gates. The three stale live-owner guards and semantic naming-map divergence are not resolved by fixture consistency. Overall migration and continual optimization remain active.
