<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3 compile-time primitive image / optional fixed-VA receiver

**Candidate, not promoted OpenJDK compatibility:** additive M3JDK21-owned Java 21 / Linux JNI implementation. No public `java.lang` or `java.util` API changes. No changes to existing `M3StringPool`, `M3StringAtom`, String interning, HotSpot object layout, or VM GC. Does not turn boxed or primitive `==` into pointer comparisons.

## Existing owners and compile-time entry

* `src/java.base/share/classes/jdk/internal/mindex/M3PrimitiveIdentity.java`: exact `(kind, rawBits)` identities for all eight primitive kinds, no boxes or per-value cells, with explicit Java float/double-`==` helpers. Values of type long/double cannot store their entire bit domain **and** a type tag in one 64-bit long; the hot representation is two primitive lanes.
* `src/jdk.compiler/share/classes/com/sun/tools/javac/m3/M3PrimitiveImageCompiler.java`: **opt-in** attributed compilation stage, reads literal trees plus `VariableElement.getConstantValue`, uses `-proc:none`, refuses errors, emits deterministic sorted image and slot-offset sidecar. No arbitrary source evaluation, no bytecode changes, no annotation processing. It does not pretend that calls or mutable state are compile-time constants.
* `src/java.base/share/classes/jdk/internal/mindex/M3PrimitiveImage.java`: validates format/count, reserved fields, canonical entry order and SHA-256 before exposing slots. Maps image read-only; consumers use image-root/slot across JVMs, not numeric pointer alone.
* `src/java.base/share/classes/jdk/internal/mindex/M3PrimitiveFixedArena.java`: **optional Linux** exact mapping adapter. Linux implementation in `src/java.base/linux/native/libjava/M3PrimitiveFixedArena.c` uses `MAP_FIXED_NOREPLACE` (never `MAP_FIXED`); errors or unavailable address fall back to ordinary image lookup. `loadForTest()` loads an isolated native test peer when running on stock OpenJDK patched for proof.

The compile stage has no automatic javac hook: execute it explicitly in a build. Compiler attribution is the source of image facts; the JVM does not need to recompute compile-time constants or run an application initializer merely to populate its optional intern table. Emitted `*.tsv` rows are `(type, raw bits, canonical slot, 64+16*slot byte offset)`; consumers can bake only validated slot numbers into generated metadata, not replace Java primitives or mutable arrays. Do not confuse this with primitive-value speedup: Java scalar equality is already cheap.

## Image format

Version 1 is big-endian, 64-byte header, 16-byte sorted immutable rows. Header: 8-byte `M3PRIM01` magic, 4-byte version, 4-byte count, 8-byte exact file length, SHA-256 of magic/version/count and all rows, 8 reserved zero bytes. Each row: kind byte (boolean=1, byte=2, short=3, char=4, int=5, long=6, float=7, double=8), seven zero bytes, raw bits as one unsigned 64-bit lane. Maximum 1,048,576 rows / 16 MiB + header. Identical input constant sets deterministically produce identical content and slot numbering. A new image produces a new namespace; **never reuse an old compiled slot without checking namespace**. SHA-256 protects against accidental corruption and provides a practical content-addressed namespace, but collision verification is required if an adversarial external registry claims identities across unrelated contents.

All primitive raw-bit rows are typed: `int(1)`, `long(1)`, and `float(1.0)` are distinct; `-0.0` and `+0.0` are distinct, and NaN raw payloads remain distinguishable when supplied by dynamic code. Java `float/double ==` is not equivalent to raw identity. Source constants are limited to values actually proved by `javac` (including static final cast expressions). Literal scans are source facts, not proof that arbitrary computations are pure. Backing files must be finalized and immutable while mapped; concurrent file replacement/mutation is not supported by this slice.

## Two processes, one virtual address

On cooperating Linux processes, mapping an identical read-only file at the same available page-aligned VA produces equal numeric addresses for the same rows:

```text
JVM A: 0x600000000000 + 64 + slot*16
JVM B: 0x600000000000 + 64 + slot*16
```

**Not guaranteed:** reservations can be occupied due to ASLR, native libraries, compressed oops, runtime placement, platform policy, address exhaustion, or sandbox restrictions. Not supported for Windows in this packet. ASLR/security policy makes fixed mapping opt-in. No same-pointer assertion is valid until both mappings succeed, namespace and slot match, the file identity is verified, and the mapping lease remains live. A stale pointer after unmap/replacement is invalid; processes need independent lifetime management. The native adapter does not touch GC-managed object addresses and never overwrites a collision.

## Executed focused proof

From repository root (requires Linux GCC for JNI check; Java 21 elsewhere):

```sh
python3 m3/runtime-integration/primitive-address-identity/verify.py
```

This executes Java 21 `-Xlint:all -Werror`, a real attributed javac pass, binary fixed point, compile-error refusal, corruption rejection, 131K+ primitive/domain checks per test process, independent JVM image-root agreement, GCC `-Wall -Wextra -Werror`, two concurrent `-Xcheck:jni` JVMs, and Linux collision refusal. On one Linux test environment two JVMs mapped at `0x600000000000`, with equal image namespaces; this is a local bounded proof, not source-built M3JDK jtreg/JCK or Windows compatibility. The proof script skips native on unsupported hosts, preserving its explicit status.

Maven verification receiver:

```sh
mvn -B -ntp -f m3/runtime-integration/primitive-address-identity/pom.xml verify
```

Remaining **PENDING**: canonical Synexia source-template recipe fixed point and full Maven/OpenRewrite run, source-built `libjava` linkage/JDK image, jtreg HotSpot interpreter/C1/C2/GC and CompactStrings, Windows/macOS fallback and native proof, corrupt shared-file race / immutable publication protocol, OS-level namespace/version negotiation, resource/RSS/JFR benchmarks, compiler source dependency seal and reproducibility in build pipelines. No global JDK intrinsic or transparent compiler lowering is enabled. Advance only after matching target evidence. Preserve String → arrays → collections → AST/compiler receiving order; this primitive metadata is a narrow dependency experiment, **not** an AST/compiler phase acceptance claim.

## Provenance

This target-specialized implementation is authored for M3JDK21. Donor ideas from existing M3StringPool/M3MappedStringBacking and Synexia compile-time text facts / MIndex bridge use canonical identities without application runtime dependency. No third-party code copied. Original target Java/native implementation files carry GPLv2-with-Classpath-exception notices; the standalone verification/doc tooling carries Apache-2.0 notice. Synexia recipe provenance and target proof must be paired before promotion.
