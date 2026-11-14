Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0

This slice fixes an unpublished exact-address mapping leak and qualifies existing Synexia UnicodeX language kernels for later M3JDK reuse.

When SHA-256 admission fails after native mapping succeeds, the original mapper leaves the requested address occupied. The pinned baseline reproducer observes the original AssertionError followed by an address-collision IOException on retry. The generated replacement releases that verified, unpublished mapping before rethrowing the original failure; the same retry then succeeds.

Two sequential Maven/OpenRewrite recipes preserve the existing public API. First, the existing hash-pinned Java AST recipe admits exactly the mapper path by replaying its own owner. Second, the generated recipe replaces exactly the pinned mapper owner. Both paths prove exact output, fixed points and refusal behavior. Existing digest-mismatch precedence, occupied-address refusal and successful process-lifetime ownership are retained. Failed map insertion is outside the speculative cleanup region because publication may already have occurred.

Qualification passed 3 registration JUnit tests, 3 mapping recipe JUnit tests, 8 actual native lifecycle JUnit tests and a separate 9-check native retry probe. Constructor-failure injection is confined to an explicitly recorded test-only private-constructor hook; the separate retry probe uses only a package declaration projection. The Windows JNI binary is an existing pinned com.m3.text build. Its preprocessed private helper/export bodies match the current source after the namespace projection. This is not a fresh jdk.m3.text binary build or proof for Linux, ARM, Android, macOS, ESP or Pico.

UnicodeX is inventoried at Synexia f2ce596a39f9a9a4ba6f48d40eb62db0a1af2757. Its unchanged five-source Java closure compiled and its existing self-test reported 83,975 checks. Caller-owned output/count loops were measured in five fixed rounds of 100,000 queries per operation: allocated bytes [1048, 0, 0, 0, 0] on Java 21.0.9+7-LTS-338. All five results were checked. These are local observations, without a portable zero-allocation or speedup claim. The first adapter's exact-zero assertion observed 888 bytes and was replaced by explicit round reporting; that diagnostic is retained.

The source identity projection retains original IDs and distinguishes canonical-byte SHA from domain-separated source identity. Word facets, derivations, corpus ingestion and native UnicodeX surfaces are catalogued with precise inspection status. The old global InternPass is reference-only; no alternate shared-storage owner is admitted. External corpora, NFC/NFKC conformance and current UnicodeX JNI execution remain unqualified. The UnicodeX sources here are immutable test fixtures, not a new runtime owner.

Reproduce the sealed local qualification with:
```text
python -X utf8 -I -S -B qualification/run.py
```
The runner assembles hash-bound readable resources into RAM, checks the existing offline 47-JAR classpath, uses the existing Maven/RAM compiler and exact archived native/image inputs, and runs five bounded stages. It requires the explicitly pinned Windows toolchain/cache paths; it does not download or copy them. Run.py is an unchanged replay helper reused from R244. A fresh platform build is still required before claiming current-ABI portability.

The executed run retained zero new files, peaked at the authorized 545 MiB total task allocation, and preserved more than 8 GiB free on K:. One exclusive temporary POM was removed after owned child processes were reaped. F: inputs stayed read-only. Full logs, packet identities, source identities, refusal results, instrumentation and storage counters are in evidence/qualification.json (zlib/hex envelope, verify expanded length and SHA-256).

Canonical convergence packet: Synexia synexia-openrewrite-recipes/crates/m3-fixed-lifetime-r245/.
Receiver mirror: M3JDK21 m3/lexicon/recipe/fixed-lifetime-r245/.
The M3JDK PR additionally installs the two recipe resources and the two generated existing source owners. No unreviewed whole-repository rewrite is included.
