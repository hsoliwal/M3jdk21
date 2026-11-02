# Object.toAdpater compatibility alias — M3JDK21
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: GPL-2.0-only -->

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

Related open PRs `hsoliwal/com.synexia#14941` and `hsoliwal/M3jdk21#2317` qualify the original `toAdapter` boundary. Do not merge or rewrite those commits implicitly. This is an independent proposal and not a claim to have completed all repository-wide optimizations.
