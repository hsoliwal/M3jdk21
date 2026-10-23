<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 — original indexed-array JNI commit fence

The original `M3IndexMaskWindow` in M3JDK21 remains the only Java owner of
bounded ordinal/mask scans. The original optional `m3_collections_mask` JNI
symbol and all public methods remain unchanged. No shadow JDK, adapter runtime,
independent arrays-of-objects, new storage owner or extra bytecode service exists.

The native branch stages up to `MAX_WINDOW` primitive `int` values. It copies
only `written(result)` values into the caller's output **after** `requireValidResult`
returns normally. Java selection remains allocation-free; before-native geometry,
mask and alias validation retains its previous order. A throwing native call or
invalid JNI cursor cannot partially publish a caller-owned output prefix.

The `src/test/native/m3_mask_fault.c` file is **deliberately broken test-only JNI**:
required mask 253 writes then throws; mask 254 writes then returns an impossible
cursor. Never package this C file as a product runtime library. A dedicated
GitHub Actions workflow builds it in an isolated temporary test directory only,
then loads it using Java's normal optional shared-library mechanism.

The actual Java/C test covers 2,200 deterministic seeded shapes in host and
`-Xint`, 8,813 assertions per JVM run, special masks, duplicate physical IDs,
indexed output offsets, rejected values, bounded source scans, forbidden bits,
all-selected fast paths, a 4096-slot scan, input failure and fault injection.
The oracle compares the Java owner with the JNI transport under `-Xcheck:jni`.
This is a focused contract check, not a full OpenJDK image/jtreg/JCK, native
production library, or performance benchmark.

The only canonical transformation recipes, SHA-pinned templates, algorithm
catalogue and rejection memory live in private `hsoliwal/com.synexia`.
The M3JDK21 PR receives **only executed output candidates and product tests**.
The Synexia Maven/OpenRewrite SDK test, source-drift/fixed-point proof, module
reactor and independent native/runtime gates remain required before promotion.
Do not merge a draft or treat queued CI as PASS.
