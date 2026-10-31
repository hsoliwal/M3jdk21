# M3 Collections: live composite streams without eager backing iterators

Canonical recipe owner: hsoliwal/com.synexia, using the EXISTING Maven/OpenRewrite class `M3HashPinnedJavaSnapshotRecipe`. Target: hsoliwal/M3jdk21 existing `com.m3.collections.M3LongCompositeViews`.

Frozen target base `e913ab89056657eca4009678cfe9a657c020e79b`, Git blob `e3e6594c62a233aedfa4d4a9bd2f36557cb4a453`; SHA-256 `759ba7905d6190f42eb691d25307dc5807af7289948fc5c45b94568d679c11b8`. Exact Java postimage SHA-256 `041c6afc3ed04671e9e9fa1a30c0ea7df7fe09aefdff405a3612afffee441ecb`. New test owners ABSENT-before. Original and new public APIs/types unchanged, no JNI symbol touched.

Problem: `cursorSpliterator(iterator(), flags)` creates source cursors (and union's k-way Merge heap) when a stream/spliterator is obtained. This contradicts the class's documented live collection-view behavior when a source changes between stream creation and traversal, while `M3LongSortedViews.sortedCursor` already defers binding. The private callback is replaced with `cursorSpliterator(this, flags)` and a lazily created cursor. This is an intentional correction of iterator-acquisition timing; observable source changes *after* binding remain subject to each source's existing fail-fast policy. No caching, O(n) index materialization, map entries or native callbacks.

The package/test code uses original first-party Apache-2.0 sources; JDK/Guava/Eclipse/Commons and LeetCode/HackerRank/GeeksforGeeks are comparison/category references only, not copied implementations. See CATALOGUE.tsv.

Pass order: source preimage and category -> exact hash-pinned Java-LST recipe -> compile -> JUnit/standalone random differential against TreeSet -> fixed point and source drift refusal -> full target Maven -> native JNI regression (UNCHANGED C17 symbol/ABI) -> installed image/jtreg. The draft target must remain HOLD until these steps actually run. No universal speed claim. Existing String -> arrays -> collections receiving order remains unchanged.
