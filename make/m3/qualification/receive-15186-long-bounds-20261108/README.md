# Receipt: com.synexia #15186 on m3/tooling/search (signed-long JNI)

#15186 (`m3(recipe): signed-long JNI prefetch and ordered merge`, head `67aa239b73fd`) is the in-place successor
of #15120 on the crate `m3jdk-sorted-long-batch-bounds-20261101`. Both merged into
`m3/superset-live-fold-independent-recipes-20261101`, and an ancestry-only fold dropped both from com.synexia
develop. #2793 received #15120's postimages. For every file, #15186's base blob equals master's, so its four
changed postimages are taken whole:

| File | #15120 (master) | #15186 |
|---|---|---|
| `src/main/native/m3_native_sorted_long_bounds.c` | `b32a59d9a8ce` | `eeefa153a39d` |
| `src/test/java/jdk/m3/tooling/search/M3SortedLongBoundsSelfTest.java` | `1f00dc0f13c1` | `8633684e2b9a` |
| `src/test/java/jdk/m3/tooling/search/M3SortedLongBoundsBenchmark.java` | `7c701754522e` | `ff1bcb2eb778` |
| `README-long-bounds.md` | `afb3e214149e` | `7db6e15ad8c2` |

The Java sources and `verify-long-bounds.sh` are unchanged. The native changes, each described in
`donor-docs/`, are:

- small-slice prefetch;
- forward-only JNI region windows for an ordered merge over large slices;
- an invocation-local memo for adjacent repeats;
- a hashed memo for nonadjacent repeats, falling back to 8-way set-associative when keys collide. Every hit
  compares the exact 64-bit key.

## Gates (OpenJDK 21 + GCC C17, Linux)

`GATES.tsv`; runners `gate.sh` and `mutants.sh`.
