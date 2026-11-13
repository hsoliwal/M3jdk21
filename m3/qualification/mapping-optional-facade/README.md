<!-- Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0 -->
# M3 mapping Optional facade experiment

**Not selected.** The proposed extraction preserves behavior but does not remove the measured 16-byte Optional allocation on warm hits. This PR changes qualification material only: no production source, storage, cache, public interface or packaged recipe registration changes.

The experiment reuses the existing M3 `Optional.ofNullable` facade pattern and canonical `M3HashPinnedJavaSnapshotRecipe`. One existing `SharedLexiconCatalog.findMapping` owner is split into a locked Optional boundary and a private nullable value helper. Optional construction remains inside the read lock; moving it outside the lock was rejected before implementation. No new cache, field, Entry/Node, representation, native code or downstream runtime dependency is introduced.

Two exact recipe variants cover current master and the pending range-reduction source from [M3 PR3011](https://github.com/hsoliwal/M3jdk21/pull/3011). Both pass typed replay, fixed point and missing/duplicate/wrong-AST/drift/post-scan-drift refusals: 14 recipe JUnit executions. The candidate is stored as text resources only and is not registered for normal recipe discovery.

Eight fresh Java21 JVMs compile the actual11-owner closure and run 13 JUnit tests each, 104 total:52 candidate and52 reference. They cover source IDs0..10000, multilingual keys, same-hash collisions/wrap, payload/source isolation, unchanged incumbent SI semantics/warm/concurrent-close methods, failure/null precedence, escaped mapping behavior and field/API fingerprints.

A separate QA child class loader instruments the actual compiled public Optional calls. The callback checks the real lifecycle read-lock hold and pauses construction until a closer is observed queued at the write lock. Hit/miss calls, close completion and thread reaping are asserted. This is a lock-order discriminator, not a production instrumentation change. The timed calls use original uninstrumented bytes.

All 336 samples use one million calls, seven samples per workload/consumer, two fresh JVMs per source variant. Hit allocation stays16bytes/query in every variant, including non-escaping consumers; misses stay0. An escaping array published before timing is the materialization control. Numeric timing sometimes improves, while master mixed-language medians worsen; the target allocation improvement fails. Times are one-host diagnostics without JMH/affinity. This harness adds escaping warmup and must not be compared directly with R231 timing results.

The unchanged range helper is checked against12,288 compiled C/Wasm/BigInteger vectors. That existing arithmetic donor is retained with its Apache-2.0 notice/license; it is unrelated to the rejected Optional optimization. No host JNI/platform result is claimed.

Input: 419a1192bf61c415ac286e561c415927e069930f. Receipt: b54088013a611a774414f838ce07c58531067da5. Uncompressed SHA-256: 7ccfc5e1cceb469e232273d8ed18b0a4309a36898b1597a79471349b6f4d8821. Verify proof length/hash before claims. `qualification-input.references.json` reconstructs the exact compact ASCII input with property order and no final LF; fetch hash-bound Git source references as required. `stdin-qualified.py` consumes its hex as1024-character lines. `qualification.py` uses the existing Maven exec plugin and RAM compiler with explicit paths and47 offlineJAR pins. The POM/bootstrap are exact reused inputs; qualification supplies the actual execution arguments rather than invoking the POM's unrelated legacy default arguments.

The existing260-byte F: image stays read-only and hash-verified. The545MiB charged task cap and8GiB reserve remain active. S: allocation is monitored and ends at0; no build files are retained. F: is charged from its sealed R230 read-only budget, not a fresh total allocation audit. The pending native-build budget increase remains unused.

Catalogue action: retain this as a correct but allocation-neutral extraction, not a runtime optimization. Future work needs new evidence before retrying this shape. The broader lexicon/M3 goal remains active.
