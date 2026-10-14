# Required JNI/C bitcount provider

This is an **optional native CPU provider**, not a new scheduler or GPU kernel.
It implements the same reviewed operation as the Java-reference bitcount lane:
`m3.parallel.int-bitcount.v1`.

The `BulkTask` contract is an exact `int[] bits` input with `workItems` elements,
`localWork=0`, and a fresh `int[] counts` result. Inputs are defensively
snapshotted before the native call. Every result element must equal Java's
`Integer.bitCount(int)` on that snapshot. Native mismatches or malformed outputs
fail before publishing a `BulkExecution` receipt.

Java loads the native library only from an explicitly configured absolute path.
Missing, wrong-ABI or differently configured native libraries are refused; there
is **no silent fallback**. The C11 JNI function operates on bounded chunks with
`GetIntArrayRegion`/`SetIntArrayRegion` and never retains heap addresses.

## Executed local contract probe

A Java 21 **interface fixture** (not full repository sources) was compiled using
`-Xlint:all,-module -Werror`; the actual new C11 JNI source compiled with GCC
14 warnings-as-errors and UBSan. Actual native calls with `-Xcheck:jni` passed
330,918 checks in the normal VM and the same 330,918 checks with `-Xint`, plus
required-missing-library refusal. The interpreter repetition is not new distinct
coverage. Inputs include signed extremes, seeded arrays, the 262,144-item boundary,
and concurrent independent requests.

Full-source replay uses:

```sh
bash m3/ports/parallel-bulk/verification/verify-native-bitcount.sh
```

This script compiles the **real** five JDK bulk owner sources as an isolated
`java.base` patch and then compiles/calls the real provider/C JNI implementation.
Separate Maven/JUnit and exact-head CI gates remain mandatory. No JNI speedup,
physical GPU execution, or complete JDK admission is claimed.

## Donor evidence

- LeetCode 191 (Number of 1 Bits): exact per-word bitcount category.
- HackerRank Day 29 (Bitwise AND): bit-manipulation shape; **different contract**.
- GeeksforGeeks count-set-bits category: range/aggregate shape; **different contract**.

Existing licensed/pinned Synexia catalogue/bitcount review supplies provenance.
These entries are EVIDENCE_ONLY; no challenge solution code was imported.
