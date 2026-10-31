# Experimental segmented java.lang.String — coordinated VM slice

Original experiment contributions: Hitesh Soliwal <hsoliwal@gmail.com>.
Modified OpenJDK sources retain their upstream GPLv2/ClassPath Exception notices.
New m3 recipes and harnesses are independently authored Apache-2.0 files.

This branch changes the actual java.lang.String representation and matched
HotSpot implementation. It is not a wrapper or a replacement String.class for an
installed VM. Always build and select the complete isolated image.

## Representation and supported producer path

With `-XX:+UnlockExperimentalVMOptions -XX:+UseM3SegmentedStrings`, after VM
bootstrap, ordinary two-reference `a + b`, String.concat and nonempty substring
can create ordinary Strings retaining immutable flat String leaves. String's
original final @Stable byte[] value remains permanently null on these objects.
Three new final fields hold the leaf String[] directory, primitive int[] ranges
and logical UTF16 length. Each range pair is leaf UTF16 start plus cumulative
logical end. Leaves must have actual nonnull original backing; no recursive
segment graphs or mutable source arrays are admitted. The directory is bounded
to 256 leaves. Publishing m3Parts last identifies a fully initialized segmented
object to VM readers. Normal reference scanning retains all leaf lifetimes.

Length, charAt, equality, hashing, intern comparisons and slicing traverse
logical code units. Slices recompute compact coder eligibility, including ASCII
slices of UTF16 leaves; malformed surrogates remain code units without repair.
Copy construction shares the immutable directory. A slice may retain a complete
large leaf array: this is backing reuse, not a total-memory reduction guarantee.

Java consumers requiring contiguous storage call value(), which may create a
separate volatile m3Flat byte[] cache. Competing computations may allocate equal
temporary arrays, but only fully initialized immutable data is published. The
original final value is never rewritten. General multiargument concat recipes,
StringBuilder/StringJoiner materialization, and the directory-cap fallback stay
contiguous. This initial producer slice does not make every `+` expression retain
segments; the actual two-reference invokedynamic route is tested explicitly.

## Native and startup boundaries

VM value() accessors remain raw, nullable and nonallocating. New logical length
and code-unit readers handle segmented objects. Native hash/equality, intern,
symbol/UTF8 conversion, diagnostics, finalizer and JVMTI String callbacks use
these readers or native temporary copies. They never invoke Java to materialize
inside weak-reference, diagnostic or no-safepoint contexts.

The native libjava UTF8 platform-path fast path also recognizes segmented Strings
and routes them through its existing Java charset conversion boundary.

JNI GetStringChars/UTFChars/regions copy logical content. GetStringCritical
returns a native UTF16 copy for segmented Strings; ReleaseStringCritical frees
that copy using the immutable segment discriminator, even if another Java thread
has populated m3Flat meanwhile. Original flat UTF16 pin/unpin behavior remains.
No pointer is retained after release. The Serviceability Agent decoder recognizes
the added fields, though live-SA attach coverage is separately reported.

The first enabled mode is interpreter-only: argument processing enforces it even
with a trailing -Xcomp. C2 concat optimization and CDS sharing are disabled;
JVMCI, string deduplication, CDS archive operations and JFR recording requests
are rejected. Dynamic JFR and CDS dump entry points are gated too. These are
explicitly unsupported capabilities, not verified compiled-mode fallbacks.

Flag-off behavior remains flat and runs with ordinary compilation/CDS/dedup.
However, the matched VM/class layout still includes extra fields, increasing
String object size even when disabled. This image is an experiment, not a
performance-neutral or binary-compatible drop-in JDK replacement. Full upstream
conformance, production hardening and additional architecture coverage remain.

## Interning and ownership scope

String.intern continues to use the real content-based HotSpot StringTable and
can retain a segmented canonical String without flattening Java backing. This
is distinct from canonicalizing every leaf or directory. No new global body
interner, bounded local arena, LRU/MRU pool, dynamic native builder or shared
lexicon was added. Existing immutable Java String backing supplies this vertical
slice. Its ordinary GC-owned lifetime does not implement Synexia native-handle
retirement, generation validation or mapped-file publication.

All three user approaches remain relevant: explicit wrappers, compiler lowering,
and custom runtime. See `synexia-reconciliation.md` for latest develop reuse and
missing connections. Native arena ownership belongs to the coordinated Synexia
lane; this branch must not invent a competing arena or claim fixed Java arrays
are resizable.

## Build, replay, tests, rollback

The source recipe is based on runtime-stage1 commit
`69f18626583dee553ba1ed6623d13af492c1b709`, ultimately official jdk-21+35
`890adb6410dab4606a4f26a942aed02fb2f55387`. Complete runtime patch and exact
pre/post hashes live in recipe/. It refuses source drift and mixed states before
applying anything, supports exact reverse replay, and tests earlier recipe gates
on an isolated prior-state snapshot. Do not run earlier-stage source checks
against this intentionally different runtime layout.

```
python3 m3/runtime-segments/recipe/apply.py --check
python3 m3/runtime-segments/recipe/test_recipe.py
# Use the isolated dependencies/boot JDK described in m3/docs/native-build.md:
make images JOBS=5
M3_BOOT_JDK=/path/to/original-java21 M3_TEST_JDK=/path/to/matched-image \
  bash m3/runtime-segments/run-focused.sh
```

Preserve the original image outside the build tree before building. Rollback
means selecting its absolute java path or JAVA_HOME/PATH. `recipe/apply.py
--reverse` restores exact prior source, followed by rebuilding the full image.
Never transplant these classes into stock HotSpot or this VM into a stock JDK.

The tests inspect private fields read-only to prove original leaf identity,
null original value and absent flattened cache after concat/slice/hash/equality/
intern/JNI. They separately test explicit cache materialization, code-unit seams,
serialization, charset/API fallbacks, cap fallback, concurrency and GC lifetimes.
Allocation probes compare flag-off -Xint against enabled -Xint with the same
matched image; their payload counts exclude object/metadata headers unless
thread-allocation totals are explicitly reported. Measured results and failures
must be reported from actual runs, not inferred from test source.
