<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: GPL-2.0-only -->

# Object.toAdapter guarded-self cast — receiving contract (draft)

## Ownership and exact source seal

Source recipe: [hsoliwal/com.synexia#15439](https://github.com/hsoliwal/com.synexia/pull/15439),
named `com.synexia.m3.ObjectToAdapterSelfCast20261102`. This M3JDK21
branch receives **only** its already reviewed full-source `Object.java`
postimage, without copying the recipe into the product runtime.

| Source | Identity |
| --- | --- |
| Public baseline commit | `master@172e39d5e69a8f7775c25ad1ce2bfaf2bf5d1246` |
| `Object.java` original Git blob | `a9899e1c2970f6d5e9172ffb12f5b7399d052b68` |
| `Object.java` original SHA-256 | `014671527cd647d90f8cab6d6dbd0ca127d918c5a8f38d1ec50be6140eb63735` |
| Reviewed recipe postimage Git blob | `c703c72bea6e792a378bb6d3babc96f22dbf6cb4` |
| Reviewed recipe postimage SHA-256 | `bd21f3da2ff9f8365576946cfe2d5b5557acaace76de2146277e9d1baa436bd4` |

A mismatched input must **refuse** application. The reviewed postimage is
the only allowable outcome, and replay on it must make no further change.

## One-file, one-method mechanical change

The existing generic method `public <T> T toAdapter(Class<T> clazz)` stays
public, inherited, nonfinal, overridable and has the same erased JNI method
descriptor `(Ljava/lang/Class;)Ljava/lang/Object;`.

Only the following branch changes:

```java
if (clazz.isInstance(this)) {
    return (T) this; // after type membership has been positively checked
}
```

The method receives `@SuppressWarnings("unchecked")` for that localized
source-level unchecked cast. The old version used `return clazz.cast(this)`
inside the identical positive branch.

Other behavior is locked:
- Null requested class throws `NullPointerException("clazz")`.
- Assignable request returns the exact same object, including arrays and interfaces.
- Exact `M3Object.class` request returns the existing lazy identity-bound capability.
- Unsupported nonassignable requests return `null` without conversion.
- Subclass overrides continue virtual dispatch through Java/reflection/JNI.
- No fields added to `Object`; no Java API signatures, native symbols,
  HotSpot vtable, object shape, GC identity, or event notification changes.
- **No** `toAdpater` misspelling alias or seventh Object vtable slot.

## Category-first algorithm/donor evaluation

- **Type membership and guarded cast:** Java 21 `Class.isInstance` and
  the existing JDK source are the authoritative compiler/runtime contract.
- **LeetCode 146/706 (LRU/HashMap):** reject direct import; cache keys and
  eviction algorithms do not model nominal type assignment.
- **HackerRank Java Instanceof / Java Interface:** test-category references,
  not code authorized to change `Object` semantics.
- **GeeksforGeeks Java reflection/adapters:** test-category reference only.
- **Eclipse Equinox adapters:** conceptual reference; no EPL-source import.
- **JNI:** follow the existing `CallObjectMethod` virtual invocation
  and already admitted `ObjectAdapterNativeTest` in M3JDK21 #2317.
No LeetCode/HackerRank/GfG solution bodies are copied.

## Qualification and stop conditions

After documentation and source apply: verify the **exact Git postimage**,
the scope-limited diff, compiler warnings, isolated Java/JNI equivalence,
subclass override and null/error cases; then run the *matched rebuilt*
M3JDK21 image's original Java/JNI jtreg, C2, class loader, GC, HotSpot,
CDS, public ABI and whole-JDK admission. The source-sealed Synexia
OpenRewrite Maven suite and runtime acceptance are independent gates.

A stock JDK21 test-only mirror or `javap` bytecode demonstration is useful
diagnostic evidence **only**, not proof of the modified product runtime or
a measured performance gain. Keep the PR draft while critical gates are
not verified.

This branch must not be merged with the overlapping, unresolved alias
and vtable changes in [M3JDK21 #2320](https://github.com/hsoliwal/M3jdk21/pull/2320).
