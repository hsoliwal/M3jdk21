<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 composite-native String tuple traversal receiver — DRAFT

**Technical priority 0:** the experimental `java.lang.String` M3-backed mode must process immutable shared native/mapped atoms and persistent tuple/range composition directly. It must not flatten solely to iterate.

## Source custody and exact lineage

- Canonical Synexia recipe: `com.synexia.m3.Jdk21TupleStreamCursor`, [Synexia PR #11220](https://github.com/hsoliwal/com.synexia/pull/11220).
- Exact Synexia authoring revision: `06e8ff9c217689709b6b166c658e78aeb434fd16`. Templates retain target's original GPLv2-with-Classpath-Exception source notices.
- Target before: `master@134e84519e3eb3d75328527e3181c4cb5adbbe45`; M3String blob `70d1d03c109126fdda36c54da21b484bbe368fd9`.
- Target post: M3String Git blob `4fb87fd70ffe5c8d75d17db9c28b44bc9b574704`; existing JTreg String stream test Git blob `4d19e74a9afe3978594eb065330e47fb3fb8b36f`.
- Target preserves all existing `String`, `M3String`, `M3StringTuple`, pool, JNI, VM flag, and native owner public/package-visible method contracts; the new reader is private.
- No source or binary dependency on Synexia at runtime. This file is target-side evidence, not a competing master recipe.

## Implementation

Inside the existing native-backed `java.lang.M3String`, stream spliterators use one thread-confined `CodeUnitCursor` per split. It descends canonical tuple children on first access or a nonlocal seek, then reads the resolved scalar owner directly over a checked visible run. The cache contains an owner reference and scalar/logical coordinates, not copied text, native pointers, or a language-masked `CharSequence`. Surrogate pairing across split/tuple boundaries, random access, `tryAdvance`, `forEachRemaining`, and parallel stream splitting continue to use existing Java 21 semantics.

## Executed versus unfinished

Executed on a **standalone model**, not a rebuilt VM: OpenJDK21 `javac --release 21 -Xlint:all -Werror`, `-Xmx32m` randomly composed and sliced tuple trees, 929,903 equality checks, 1,657,752 observed old-model recursive tuple visits, three broken candidates rejected. Source- and hash-sealed two-target replay model: 4 partial-resume states, 6 refusals and zero fixed-point changes. Separate target-test-shaped source compiled with host Java21. No general speedup or M3JDK full runtime result is claimed.

**NOT RUN / PROMOTION HOLD:** M3JDK21 full product image build, `test/jdk/java/lang/String/M3StringStreamsTest.java` jtreg under a matched `-XX:+UseM3StringStorage` VM, HotSpot C++ compilation, JNI/GC/JIT/CDS/JVMTI/native tests, OpenRewrite Maven SDK, full static analysis, platform/race/performance gates. The experimental VM flag remains default-off, interpreter-only, and carries the existing restrictions.

Keep this PR draft and do not rebase, squash or force-push the target's mainline. Run the receiver and source-seal checks from the pinned recipe before any promotion.
