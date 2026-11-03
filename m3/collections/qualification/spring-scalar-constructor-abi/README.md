<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 Spring scalar view: retain JVM constructor ABI (2026-11-03)

## Scope, source seals, and authority

**Status:** DRAFT/HOLD, not product-merge-qualified.
**Receiving repository:** `hsoliwal/M3jdk21` (this branch only).
**Canonical Synexia recipe and test owner:** [#15537](https://github.com/hsoliwal/com.synexia/pull/15537). **Related existing receiver:** [M3JDK21 #2170](https://github.com/hsoliwal/M3jdk21/pull/2170).
**Single production source:** `m3/collections/implementations/spring/src/main/java/com/m3/collections/spring/M3SingleValueMapView.java`.
**Verified preimage Git blob:** `e397cb4ab04246e34449a278f0149aa718106622`.
**Exact reviewed postimage Git blob:** `bf28adb8154e25ef64bce8ad8f5b55bce46d70bb`.
**Preimage SHA-256:** `f79e36374fac2033d9292e4806254272faabdc54fff9d4fc6ebd8bbcba0f681b`.
**Postimage SHA-256:** `c79ba4f515aca777b1ba9049c3c9ec079832c22238b008a4dfddbfa3e5badb18`.

No original publicly observable APIs may be removed. Earlier code exposed the public constructor JVM descriptor `(Lcom/m3/collections/spring/M3MultiValueMap;)V`; a later source change widened the declaration to `(Ljava/util/Map;)V`. Although Java source calls may compile, existing client bytecode still refers to the old descriptor and throws `NoSuchMethodError`. The one approved leaf *adds back* the old constructor and delegates to the retained widened Map constructor, preserving its null rejection. No other Java method, object identity behavior, serialization UID, Java Map.Entry facade, key/value array storage, native C body, JNI symbol, allocator or JDK java.base code is altered.

## Proof already executed before receiving

A standalone Java 21 type-shaped differential was rerun for this receiving proposal in a fresh isolated workspace with `--release 21 -proc:none -Xlint:all -Werror`, `java -ea -Xverify:all`, JIT and `-Xint`, and `javap -public -s`.
- Old precompiled client against the original concrete-constructor descriptor: **PASS**.
- Unmodified old bytecode against generalized Map-only constructor: **expected RED `NoSuchMethodError`**.
- Same old bytecode against dual-constructor postimage: **PASS** in JIT and interpreter; exact original and repaired consumer outputs equal.
- Input Java source pre/post SHA-256 unchanged; shell lint passed.

These checks model the *type skeleton*. They are not a claimed full compilation of original Spring 6.2 dependencies or the installed M3JDK21 release. Existing JNI mask differential proofs are independent and do not authorize this Java library promotion.

## Serial acceptance — HOLD

Source Git blob match → canonical Maven/OpenRewrite fixed point and drift/missing/duplicate refusal (currently NOT RUN) → strict original Spring module compilation → original and new Spring provider JUnit and serialization checks → JDK21 binary ABI `javap -s` against actual compiled classes → full M3 collection build / JNI runtime checks → CI green → review/merge. Historic source receipt mismatches must be reconciled via **new qualified successor**; do not modify immutable historical manifests, suppress failed CI or infer equivalence without diff/hash.

## Donor category review

LeetCode 706 (hash-table put/get/remove), HackerRank Sparse Arrays (string-key frequency) and GeeksforGeeks hashing/constructor overloads give **problem categories and adversarial test shapes only**. Actual source is the existing M3JDK21/Synexia licensed first-party implementation. **Zero challenge solutions or third-party source are copied.**

No rebase, force-push, master edit or source-history replacement is part of this proposal.
