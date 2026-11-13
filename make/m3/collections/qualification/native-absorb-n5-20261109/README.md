<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# NATIVE_ABSORB N5: mat CSR transpose and int filter C (2026-11-09)

This leaf absorbs the C side of two `jdk.internal.m3.collections.mat` owners from com.synexia develop
`537b10a9de1f`. `NATIVE_RELOCATE.tsv` records the blobs.

| M3 owner | Donor C | Library | Load |
|---|---|---|---|
| mat.MatCsrGraph | synexia-mat-collections/src/main/c/m3csrtranspose.c | m3_mat_csr | `m3.matcsr.native.path`, or else `System.loadLibrary("m3_mat_csr")` |
| mat.MatIntFilterKernel | synexia-mat-collections/src/main/native/mat_int_filter_jni.c | m3_matfilter | opt-in `m3.matcollections.jni`, then `System.loadLibrary("m3_matfilter")` |

NATIVE_SYMBOL_RELOCATE v1.1 renames the JNI prefixes and the generated-header include. make/m3/collections builds
both libraries against the force-included M3 javac -h headers.

No test in the module exercises either native, so this leaf is gated at build level only: exports equal the census
BOUND set, and the prototype mutant is killed.

## Census

BOUND 187 -> 189, ABSENT 16 -> 14.

Seven methods are left:
- MatBitMatrix: 2 methods. N7b retires it in favour of jdk.m3.arrays.BitMatrix, which has C.
- MatIntRowKernel: 2 methods. No C found yet.
- MemoryPressure: 2 methods. No C found yet.
- MarkerTopKNative: 1 method. No C found yet.

MatIntPostingIndex accounts for the other 7 methods; N7b retires it in favour of jdk.m3.arrays.IntPostingIndex,
which has C.
