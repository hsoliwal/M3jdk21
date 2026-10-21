# M3JDK21 composite-path algebra invariant (2026-10-21)

M3JDK21 is the integrated Java 21/HotSpot/JNI SDK, not an M3+JDK wrapper.
Canonical backing is the existing parent-owned Object[] or primitive array and,
where appropriate, M3String interned native/OS-backed owner. A composite
path is a composition of existing owners, integer offsets, physical indexes,
order/rank, bit masks, state words, bounded child descriptors and navigation
rules. It is NOT a separate store of copied elements or linked per-entry nodes.

The existing source owner M3LongCompositeViews.Concatenation now walks nested
child M3LongCollection[] arrays with private dynamic frame and index arrays,
without recursive wrapping or flattened retained payload. Leaf cursors bind
only on traversal; size, contains, isEmpty, iterator, toArray and streams
preserve encounter order, duplicate occurrences, liveness and read-only policy.
Traversal is O(path-descriptors + selected values) with O(depth) transient
frame metadata; no new public/protected API, JNI export or per-element boxing.

Other path operations (slice, filter, sorted projection, reverse, search,
hierarchical dynamic programming) may compose only if equality, mutation,
callback order, exception semantics and ABI are independently proven.
JNI known-slice materialization may copy source byte/char slices into
conventional contiguous arrays at explicit API boundaries. Java array exports
must not be portrayed as zero-copy discontiguous aliases.

Synexia invariant: catalogue algorithms by problem category, qualify licensed
GitHub donors, LeetCode/HackerRank/GeeksforGeeks shapes and existing implementations;
perform serial contract-preserving file-local behavioral-atom refinements
through an improved and reusable Maven/OpenRewrite source-sealed recipe.
Retain before/after hashes, drift refusal, second-run fixed point and
per-file compiler/JUnit/JNI/runtime proof before promotion. Recipes remain in
com.synexia; M3JDK21 owns product runtime. String -> arrays -> collections
phase sequencing and exact existing public contracts remain unchanged.

THIS IS NOT a universal path-algebra runtime/compiler or full SDK convergence.
A Java21 independently authored *model* passed 43,847 path checks including
32768 nested frames; owning M3JDK21 Maven tests, full reactor, ABI/JNI and
installed image remain unexecuted for this candidate until proof receipts exist.
