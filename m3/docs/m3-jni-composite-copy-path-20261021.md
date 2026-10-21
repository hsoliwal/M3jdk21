# M3JDK21 JNI composite leaf-path materialization

**M3JDK21 is one integrated Java 21/HotSpot/JNI SDK, not an "M3 + JDK" wrapper.**

Existing canonical immutable spelling is parent-owned: M3StringOwner,
M3StringAtom/Immediate/Tuple and compact M3String owner/coordinate identity.
Existing JNI String.c nativeByteShadow/nativeCharShadow each create exactly
one final mutable contiguous Java byte[] or char[] as an explicit export.

M3StringTuple.getBytes/getChars now use the existing M3StringPool.LeafCursor
to process exact known scalar owner/range slices. LeafCursor already walks
the persistent composition DAG using growable M3StringOwner[] and long[]
coordinate lanes; adjacent physical scalar slices can coalesce. Source arrays,
native/mapped atom addresses, strong owner/lifetime rules and logical String
identity do not change. No intermediate joined String, per-segment byte[],
per-element heap Node/Entry or second payload store is created. JNI code
releases its invocation-local GetObjectClass reference after GetMethodID.

Original destination bounds, UTF-16 code units, LATIN1/UTF16 coders,
unpaired surrogate preservation, java.lang.String public contract,
native method symbols and one-allocation shadow semantics are retained.
The scalar leaf getBytes/getChars implementation remains the authority for
format conversion and JNI/GC-safe native-memory access. This change does NOT
replace byte/char array compatibility with a rope and does NOT claim byte/
char memory images alias each other.

Synexia owns the exact Maven/OpenRewrite recipe, before/after hashes,
problem-category catalogue, reusable tests and failed proof receipts.
M3JDK21 owns Java/HotSpot/JNI runtime and final qualification.
The donor category is ordered immutable composite range traversal.
Existing LeafCursor is selected over LeetCode 341, HackerRank preorder,
GeeksforGeeks iterative traversal and the pinned MIT TheAlgorithms Java
preorder code: their eager collections or linked node stacks violate M3
owner/index/mask constraints. No donor source is copied.

Must still prove: actual Maven/OpenRewrite SDK recipe application, Java 21
java.base source build, existing String/JNI jtreg suites, new composite JNI
shadow jtreg, native -Xcheck:jni, compiler/HotSpot/GC invariants, installed
M3JDK21 image, and comparative heap/native/CPU benchmarks. No merge or
promotion on source hash receipts alone.
