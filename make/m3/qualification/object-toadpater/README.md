# Object.toAdpater compatibility alias — M3JDK21
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: GPL-2.0-only -->

## Cache and installed JNI follow-up, 2026-11-02

[CACHE_QUALIFICATION.md](CACHE_QUALIFICATION.md) and [CACHE_QUALIFICATION.json](CACHE_QUALIFICATION.json) record the later indexed weak-owner and bounded-plan implementation, actual installed-libjava qualification, loader-lifetime and churn regressions, and the repeated nonfinal alias compatibility matrix. The exact requested `toAdpater` API and seven-slot Object vtable are retained. Measurement limits and memory tradeoffs are stated alongside the results.

## Forward qualification, 2026-11-02

The source-pinned follow-up preserves this nonfinal candidate and its historical
binary fixture. [QUALIFICATION.md](QUALIFICATION.md) and
[QUALIFICATION.json](QUALIFICATION.json) record the later actual recipe, source
build and runtime evidence, source identities and remaining gates. The original
[RECEIPT.json](RECEIPT.json) remains the historical source/model-only record for
commit `008a84b7e5592888f78e67318a1c08863a517109`.

The expanded existing native fixture exercises both overridable method names,
abstract-interface dispatch, both-method override chains, null-accepting overrides,
exception identity, generic result boundaries and class-loader identity. Object's alias
Javadoc makes null rejection depend on the selected canonical implementation.
The workflow below remains a source/host-fixture lane; it does not build M3JDK.

This is a separate, additive API candidate; it does not rename or remove the already implemented `public <T> T toAdapter(Class<T> clazz)`. The requested exact public name is `toAdpater`, and both signatures erase to `(Ljava/lang/Class;)Ljava/lang/Object;`.

## Semantic contract
* `public <T> T toAdpater(Class<T> clazz)` is non-final and delegates **virtually** to `toAdapter(clazz)`. Subclasses may override either method.
* Assignable identity, exact lazy `M3Object` adaptation, unsupported-null, null-token `NullPointerException`, and existing subclass dispatch remain owned by `toAdapter`.
* No new Object fields, registry, native Object entry points, or changes to other Java SE 21 APIs.
* HotSpot's `ClassLoader::compute_Object_vtable` must reserve seven virtual entries (five legacy, `toAdapter`, `toAdpater`), including array klasses.
* JNI must resolve the exact erased method descriptor and use virtual `CallObjectMethod`, `CallObjectMethodA`, and `CallObjectMethodV`; no nonvirtual substitution.

## Required qualification (not claimed by this document)
Inventory exact Git source blobs and hashes; apply the documented source diff; run lint, full M3JDK compiler/HotSpot build, jtreg Java/JNI tests with `-Xcheck:jni` in default, `-Xint`, and C2, old-subclass binary compatibility, existing adapter and array regressions, and source-sealed fixed-point recipe replay. Keep failure logs and image IDs. No installed-image completion without these gates.

## Donor decisions
The existing Synexia `M3AlgorithmRecipeFirstFullReviewRecipe` is the catalogue owner. LeetCode706 (hash-table design), HackerRank Java `instanceof`/reflection, and GeeksforGeeks adapter-pattern references inform test classification only. Their editorial/submission code must not be copied. Existing M3JDK21 `Object` and `M3Object` semantics are authoritative; Oracle's JNI specification determines native calling behavior.

The historical Synexia PR `hsoliwal/com.synexia#14941` (closed without merge) and M3JDK PR `hsoliwal/M3jdk21#2317` qualify the original `toAdapter` boundary. Do not merge or rewrite those commits implicitly. This is an independent proposal and not a claim to have completed all repository-wide optimizations.

## Pre-existing subclass binary regression (successor qualification)

The Java 21 class file `LegacyPreAliasSubclass` is compiled once against unmodified OpenJDK 21, which **does not** declare `Object.toAdpater`. It already declares the public erased method `toAdpater(Ljava/lang/Class;)Ljava/lang/Object;`. Embed the exact historical class bytes in a new jtreg test and load them through an independent class loader; invoke the new base-typed alias to require JVM virtual dispatch to the old subclass method. Source is first-party, retained in the qualification package.

- Stock-JDK-produced classfile major version: 65.
- Frozen class-file SHA-256: `4eff8d88c8154820bee15152e0f2cb4791b305cc67b5675b34276310aa568e36`.
- Baseline fixture is **not** recompiled under the modified M3JDK.
- Independent Java 21 model: a previously compiled subclass invokes correctly when a base alias is introduced as non-final. Replacing only the model base with a final alias causes the expected `IncompatibleClassChangeError`.
- This model does **not** establish actual M3JDK jtreg or HotSpot image success.

The historical `m3/compatibility/check_object_adapter_progress_receiver_20261017.py` source seal remains unchanged and intentionally refuses the evolved `Object.java` SHA-256. GitHub Actions for the initial alias PR reported this refusal; it is not converted into a green result. A separate successor acceptance receipt must pin the new source identity without weakening that historical check.

## Strict scoped verifier and host-JDK fixture lane

`verify_candidate.py` is an **additive source-identity** gate for exactly five candidate product/test paths, pinned to their reviewed Git blobs. It checks the public alias signature and the seven-slot HotSpot source constant without replacing, modifying, disabling, or widening historical admission guards. The verifier emits `SOURCE_DRIFT` on any unreviewed blob, rejects missing or duplicate declarations, and is idempotent on the same checkout.

The first-party `LegacyPreAliasSubclass.java.txt` is deliberately **not** compiled as part of the M3JDK source tree: a separate host OpenJDK 21 compiler constructs its historical classfile and checks SHA-256 against the frozen embedded bytecode. `LegacyImageFixtureOracle.java` loads the class and tests the old generic method on a stock JDK. The host lane also syntax-checks the new C17 JNI fixture against the stock JDK JNI headers.

All of these checks are **source/fixture** checks, not jtreg execution on an M3JDK image. The new GitHub Actions workflow is strictly scoped to this candidate and may remain queued or fail independently. Historical source-drift failures remain visible.
