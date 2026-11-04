<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: GPL-2.0-only -->
# Guarded self-cast receiving verification — 2026-11-03

## Exact production result

- Base: `master@172e39d5e69a8f7775c25ad1ce2bfaf2bf5d1246`.
- Authored recipe: [Synexia #15439](https://github.com/hsoliwal/com.synexia/pull/15439);
  recipe `com.synexia.m3.ObjectToAdapterSelfCast20261102`.
- Product preimage `Object.java` Git blob: `a9899e1c2970f6d5e9172ffb12f5b7399d052b68`.
- **Product received postimage** Git blob: `c703c72bea6e792a378bb6d3babc96f22dbf6cb4`.
- GitHub connector fetched canonical recipe template at exact Synexia head
  `431ae1649136cff7d036c1bcdb2b4be59d502fc4`; applying the sole
  original-method replacement reproduced it byte-for-byte. GitHub updated
  the target, reported the same postimage blob and fresh target readback
  independently matched.
- Source change is the original `Object.toAdapter` method's
  `clazz.cast(this)` → `(T) this` on the already true `clazz.isInstance(this)`
  branch and method-local `@SuppressWarnings("unchecked")`.
- Base-to-head diff: **one modified source file, two added lines and one
  deleted line**; this document family is a separate passive evidence addition.
  No Java signatures, JDK native sources, HotSpot, or JNI declarations changed.

## Strict local proof — stock JDK21 surrogate (not product runtime)

A first-party standalone parity fixture compared exact original and candidate
method bodies under Java 21 and called both through JNI virtual dispatch.
A test-only `IdentityHashMap` capability owner substituted for the actual
M3JDK weak identity cache. Executed under
`/mnt/data/m3_selfcast_20261103` with immutable output logs:

| Stage | Executed result |
| --- | --- |
| JDK 21 `javac --release 21 -proc:none -Xlint:all -Werror -h` | PASS |
| C17 syntax `-Wall -Wextra -Werror -Wpedantic -fPIC` | PASS |
| JNI shared-library link | PASS |
| Default Java21 `-Xcheck:jni` | PASS, 180,243 assertions |
| Interpreted Java21 `-Xint -Xcheck:jni` | PASS, 180,243 assertions |
| Forced C2 `-Xcomp -Xbatch -XX:-TieredCompilation -Xcheck:jni` | PASS, 180,243 assertions |
| `javap -c` guarded-self class cast count | Original 2, candidate 1 |
| Deliberate mutant replacing guarded `clazz.isInstance` with unconditional true | **Expected failure detected**: candidate mismatched `String.class` |
| Remote full `Object.java` readback and scope diff | PASS |

Fixtures exercise assignable identity, interfaces, unsupported reference /
primitive / void, stable capability identity, null token NPE, subclass
virtual override, reflection, method handles, JNI `CallObjectMethod`,
and preservation of a subclass exception's reference identity.

A local scripted source seal initially rejected a **mistyped expected Git blob
literal** before any source mutation. The literal was corrected to the fetched
baseline; the second fail-closed run reproduced the canonical postimage and
the connector accepted the exact update. This is recorded to preserve history.

## Unexecuted/held acceptance

**NOT RUN:** Synexia #15439 actual Maven/OpenRewrite Recipe.run SDK test;
native build of a matched patched `M3JDK21` image; original
`ObjectAdapterNativeTest` against that image; original and new jtreg across
observation enabled/disabled and C2; HotSpot vtable ABI, GC/weak lifetime,
classloader, CDS, JCK and whole-JDK release gates; independent performance
benchmarks and cross-platform qualification.

A stock-JDK test-only JNI class method ID is **not** proof of the
modified `java/lang/Object` native descriptor at runtime. The bytecode
instruction reduction does **not** establish elapsed-time or allocation
improvement. Do not merge this draft until product acceptance passes.

## Conflict/handoff constraints

The earlier M3JDK21 [#2320](https://github.com/hsoliwal/M3jdk21/pull/2320)
has an unresolved `toAdpater` alias / extra Object vtable proposal,
which is not admitted here. An exact binary/API check is required before
combining these branches. No previous source history was rewritten.
