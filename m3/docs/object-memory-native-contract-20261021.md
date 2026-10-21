<!-- SPDX-License-Identifier: Apache-2.0 -->
# Direct M3JDK21 object memory introspection — additive java.base candidate

This is the **actual M3JDK21 Java 21 runtime/SDK**, not an M3 adapter layered over stock OpenJDK. The new `jdk.internal.misc.M3ObjectMemory` is an experimental internal Java API paired with JNI exports compiled directly into `java.base`'s `libjava`. Existing `java.lang.Object`, JNI and HotSpot interfaces are not altered. The canonical Maven/OpenRewrite source-and-test recipe is under Synexia PR [#12668](https://github.com/hsoliwal/com.synexia/pull/12668); this is its reviewable *product receiving candidate*, not a claim of qualified receiver installation.

## Implemented candidate operations

| M3JDK API | Implementation | Interpretation |
| --- | --- | --- |
| `shallowSize(Object)` | Native JVMTI `GetObjectSize` | Shallow VM object size, not children or native backing |
| `objectId(Object)` | Native JVMTI `GetTag/SetTag` | GC-stable, on-demand, VM-scoped opaque tag, not a pointer |
| `referenceSnapshot(Object)` | JVMTI `FollowReferences` from GC roots | Expensive observed incoming heap/root edges; not a maintained count |
| `addressOf(Object)` | Explicit `UnsupportedOperationException` | Raw moving oop unavailable until safepoint-safe HotSpot hook |
| `sizingAvailable()/taggingAvailable()` | JVMTI startup capability probes | No fabricated fallback results |

Internal usage, after the **matched M3JDK21 image** has built and passed its tests:

```java
// Experimental JDK internal API; not part of the standard Java SE surface.
import jdk.internal.misc.M3ObjectMemory;
long bytes = M3ObjectMemory.shallowSize(new int[128]);
long stableTag = M3ObjectMemory.objectId(new Object());
var sample = M3ObjectMemory.referenceSnapshot(new Object());
// raw addressOf throws until a VM safepoint-scoped facility is qualified.
```

External Java code needs appropriate `--add-exports java.base/jdk.internal.misc=...` flags; no stable public Java SE compatibility guarantee exists yet. The separate Synexia M3 lease registry tracks *cooperative native bytes/borrowers* and is not silently substituted for JVM heap referrers.

## Source provenance and tests

- Java receiver: `src/java.base/share/classes/jdk/internal/misc/M3ObjectMemory.java`, SHA-256 `b64f77925ee6cbeb4fc882b237c8eb504919cbbaf99f3b69e4bcd2c97739449c`.
- JNI receiver: `src/java.base/share/native/libjava/M3ObjectMemory.c`, SHA-256 `ff317b645e6b497f40b7b517a1f11a16fb10896ab4e61325dfc3601434b63c03`.
- jtreg receiver: `test/jdk/jdk/internal/misc/M3ObjectMemoryTest.java`, SHA-256 `259b99369196525639d27a1a13e82ac967b69691e3f486ac153fce744a786875`.

The same source identities are stored as ABSENT-before, exact after-images under Synexia's existing class-backed `M3HashPinnedJavaSnapshotRecipe` and `M3HashPinnedTextSnapshotRecipe`. Review recipe-first before promoting these exact product copies. Do not modify develop/master, rebase or force-push this evidence branch.

### Focused evidence versus outstanding acceptance

**PASS locally** on a stock Linux JDK21 test VM: Java `--patch-module java.base`, strict GCC C11, checked JNI, default/SerialGC/uncompressed-oops; on a *temporary copy* only, `System.loadLibrary(\"java\")` was replaced with the isolated `libm3_object_memory` test-library name. Tests covered shallow byte size, arrays, GC-stable identities, observed root edges, null handling and refusal of unsafe raw address export.

**NOT_RUN / HOLD**: Maven/OpenRewrite JUnit recipe scan and fixed point, source-built M3JDK21 `libjava` symbol binding, actual M3JDK21 jtreg/JCK, class bootstrap, full collectors/JIT/JVMTI/OS matrix, stable-memory/counter overhead, or native allocator integration. This draft must not be merged as a qualified installed runtime feature without those gates.

Exact instantaneous Java heap reference count requires a different invasive HotSpot side-table/barrier/root/JIT/GC design; this feature never claims it. Similarly, JVMTI tags are not memory addresses and cannot be dereferenced or freed.
