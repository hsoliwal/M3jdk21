<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# NATIVE_ABSORB N6: marker top-k and mat row C (2026-11-09)

The pinned donor commits named in the classes' lineage no longer resolve as module paths: com.synexia renamed its
`m3-*` modules back to `synexia-*`. The C was found instead by a recursive tree search of com.synexia develop
`00bd2142b0ce`, and NATIVE_SYMBOL_RELOCATE v1.1 copies it (`NATIVE_RELOCATE.tsv` records the blobs).

| M3 owner | Donor C | Library | Load |
|---|---|---|---|
| sketch.MarkerTopKNative | synexia-primitives/native/m3_marker_topk_jni.c | m3_marker_topk | `System.loadLibrary` |
| mat.MatIntRowKernel | synexia-mat-value/src/main/native/mat_int_row_jni.c | m3_mat_row | host, by absolute path with a SHA-256 pin |

The donor built the row kernel as `mat_int_row_jni`. M3 names it `m3_mat_row`, which matches `m3_mat_csr` and
`m3_matfilter`. The host passes an absolute path, so the library name does not affect loading.

## Census

BOUND 189 -> 192, ABSENT 7 -> 4. The four methods left need decisions rather than C:
- **MatBitMatrix (2).** It is a duplicate of jdk.m3.arrays.BitMatrix, which already has C. Retiring it needs
  MatAdaptiveIntGraph to return IntPostingList (see lane-mat-one-owner-20261109).
- **MemoryPressure (2).** libm3_residency was deferred by recipe residency-paged-array-20261107. It needs an owner
  decision against java.base's jdk.internal.platform.CgroupMetrics.

## Gates

See GATES.tsv.
