# M3JDK21 current-master mask viewport: direct checked-JNI qualification

## Purpose and source ownership

This is an additive, **product-local Java/JNI test lane** in M3JDK21,
not an M3 adapter layered over stock Java. The real `m3/collections`
owners are preserved byte-for-byte:

| Owner | Exact current-master Git blob |
|---|---|
| `src/main/c/m3maskviewport.c` | `f29d081e28ba8ccc4516bd28c09e738b7a408a76` |
| `M3MaskViewportNative.java` | `9cdfab35cf719ccf26f61ee61e1ae10fa98eb9ad` |
| `M3MaskViewportKernel.java` | `5966bffacc730b3ef7102d615a2331a42ae4a22b` |

No public Java API, in-JDK `java.base` class, JNI symbol, C implementation,
mask interpretation, runtime dependency or source ownership is changed.

The new independent contract program
`M3MaskViewportCheckedJniContract.java` calls the three **actual**
`M3MaskViewportNative` JNI methods directly after `System.load`:

- `project`: compare with the pre-existing Java kernel and validate selected slot order, signed 32-bit bit masks and unchanged output sentinel tails
- `projectPage`: compare with the Java kernel over nonzero input offsets, skip/limit boundaries, exact capacity and aliasing
- `projectFramePage`: independently construct expected row indices, physical ordinals and state words, including empty and large page windows

The test covers lengths 0, 1, 127/128/129, 255/256/257,
511/512/513 and 1024; random state words, contradictory required/
forbidden masks, high-bit masks, aliases (where supported) and source
errors that must refuse without output writes. It also rejects aliased
frame-output buffers, as required by the JNI implementation.

## Reproduction and gates

With Java 21, GCC, Maven and Linux JNI headers available:

```sh
bash m3/collections/qualification/checked-jni-mask-viewport-20261028/verify.sh
```

The script pins all four participating source Git blobs and builds
`libm3maskviewport.so` from the unchanged C owner under
`-Wall -Wextra -Werror -Wpedantic -fsanitize=undefined`.
The actual `m3-arrays` and `m3-collections` Maven owners compile
in dependency order; the test then executes **with `-Xcheck:jni`**
against that library under both G1 and Serial GC.

GitHub workflow:
`.github/workflows/m3-checked-jni-mask-viewport-20261028.yml`.
It preserves the selected raw compiler/native/JVM log as an artifact.

This adds neither an external JNI transport layer nor a dependency for
downstream users. M3JDK21 product build/compactor jtreg acceptance,
the Synexia source recipe, and this M3 collections JNI lane are
separate qualification domains.

## Legal/donor catalogue

First-party Java/C owners are the implementation donors; no
LeetCode, HackerRank, GeeksforGeeks or external GitHub solution was
copied. The test uses the Java implementation as an independent
oracle for `project` and `projectPage`; `projectFramePage`
uses a separate straightforward reference loop.

| Algorithm category | Owner | Decision |
|---|---|---|
| Bounded primitive array scan | Existing `M3MaskViewportKernel` | Preserve and reuse as Java oracle |
| JNI region transfers and single publication | Existing `m3maskviewport.c` | Preserve source; exercise actual linked native functions |
| Viewport/pagination by selected rank | Existing `projectPage` contract | Verify exact skip/max bounds, alias and sentinel properties |
| Triple-array projected frame | Existing `projectFramePage` contract | Independently compute row/ordinal/state result |
| Java object reference compaction | `jdk.internal.mindex.M3CollectionMaskCompactor` | **Separate in-JDK component**; not JNI, no Java reference handoff |

## Evidence boundary

The workflow and tests are submitted for review; **no passing result
is claimed until the exact PR head executes them**. This is a selected
Linux x86-64 C/JNI Java21 run. It is not full M3JDK21 fastdebug HotSpot
JNI/GC qualification, full jmods, other CPU/OS, synchronized mutation,
every native function, nor measured performance. Native synchronization
and class loader lifetimes require their own tests.

A future verified result must be recorded with the exact source Git SHA,
compiler/runtime identities, `-Xcheck:jni` output, C sanitizer output
and CI artifact link. Passing `M3CollectionMaskCompactor` jtreg in
`java.base` cannot substitute for this distinct JNI proof.
