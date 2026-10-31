<!-- SPDX-License-Identifier: Apache-2.0; Copyright 2026 Hitesh Soliwal and contributors -->
# Choosing an algorithm with java.util.Sorts

This additive M3JDK21 candidate exposes 34 named in-memory algorithms through
java.util.SortAlgorithm and java.util.Sorts. The existing Arrays, Collections,
TimSort, ComparableTimSort and DualPivotQuicksort owners keep their current behavior.
The finite registry and exact variants are documented in [the catalogue](README.md)
and [its machine-readable table](algorithms.tsv).

## Direct array sorting

~~~java
import java.util.SortAlgorithm;
import java.util.Sorts;

int[] numbers = {7, -2, 7, 0};
Sorts.sort(numbers, SortAlgorithm.INTRO);

long[] signedKeys = {Long.MAX_VALUE, 0, Long.MIN_VALUE, -1};
Sorts.sort(signedKeys, SortAlgorithm.LSD_RADIX);

String[] words = {"pear", "apple", "apple"};
Sorts.sort(words, SortAlgorithm.TIM);

// The interval is [fromIndex, toIndex); the surrounding elements are untouched.
Sorts.sort(numbers, 1, 3, SortAlgorithm.HEAP);
~~~

All eight primitive array types are supported: boolean, byte, short, char, int,
long, float and double. Object arrays support natural ordering or a Comparator.
A null object comparator requests natural ordering; required arrays, algorithms,
key functions and indexed comparators must be non-null.

~~~java
record Row(String category, int originalPosition) {}
Row[] rows = {
    new Row("b", 0), new Row("a", 1), new Row("a", 2)
};
Sorts.sort(rows, java.util.Comparator.comparing(Row::category),
           SortAlgorithm.MERGE);
~~~

The two "a" rows retain their original order because MERGE is stable.
SortAlgorithm.isStable(), family() and requiresNumericKeys() expose the registry's
metadata. The six numeric algorithms reject comparator-only and object entry
points with IllegalArgumentException. Use numeric keys when a row has a suitable
ordering key.

## Permutations and caller-owned data

order returns source indices in sorted order without moving the source.
Indices are absolute positions in the original array or indexed source, including
when sorting a subrange. There is one entry per input occurrence.

~~~java
long[] priority = {30, 10, 20};
int[] permutation = Sorts.order(priority, SortAlgorithm.LSD_RADIX);
// permutation is [1, 2, 0]; priority remains [30, 10, 20].

int[] selected = {2, 0, 1};
Sorts.sortIndices(selected,
    (left, right) -> Long.compare(priority[left], priority[right]),
    SortAlgorithm.TIM);
// selected is [1, 2, 0].
~~~

sortIndices sorts caller-provided labels. Labels can be negative, repeated or
unrelated to Java array bounds if the caller's comparator accepts them. The
library does not turn those labels into internal storage offsets.

An IntBinaryOperator compares two source positions without requiring wrapper
objects. An IntToLongFunction supplies a signed numeric ordering key:

~~~java
int[] byKey = Sorts.orderByLong(
    0, priority.length, index -> priority[index], SortAlgorithm.MSD_RADIX);
~~~

On a successful call, orderByLong evaluates the key once for every admitted source
position and caches the result in a primitive long array. Numeric key order uses
Long.compare semantics, including the complete signed long domain. Comparison
algorithms can also operate on these cached keys.

## Numeric domains and floating-point order

COUNTING and PIGEONHOLE require a key span of at most
Sorts.MAX_HISTOGRAM_SPAN (1,048,576 distinct integer positions). A span is
maxKey - minKey + 1, checked without signed overflow. A wider span refuses before
payload mutation; there is no fallback to another algorithm. The histogram limit
also applies to encoded floating-point keys, whose numeric distance can be wide
even for a short input.

BUCKET, LSD_RADIX, MSD_RADIX and AMERICAN_FLAG accept the full signed key domain.
The bucket implementation uses ordered numeric buckets and stable insertion
within each bucket; its worst case remains quadratic.

Floating-point results follow Float.compare and Double.compare ordering: negative
zero precedes positive zero and all NaNs compare equal after positive infinity.
Only ordering keys are canonicalized. Payloads move by ordinary Java copying,
which can quiet signaling NaNs on some processors. Raw NaN bit preservation
measurements therefore apply to the recorded VM and processor, not every platform.

## Work reporting and failure behavior

Convenience calls borrow ProgressMonitors.current(). An overload accepting a
ProgressMonitor borrows that monitor; it does not call begin() or done().
Passing null or ProgressMonitor.NONE explicitly disables monitoring, including
thread-interruption checks. An enabled monitor receives batched logical work
reports and cooperative cancellation checkpoints.

Sorting first prepares a primitive index permutation. Caller comparators, key
functions, cancellation checks and progress callbacks finish before a mutating
entry point moves payload values. If one of those callbacks fails before commit,
the library has not moved the caller's array. The final permutation commit invokes
no callbacks and does not poll cancellation.

Callbacks must not mutate the source or its ordering keys, and the caller must
provide exclusive access throughout the operation. This boundary does not promise
transactionality against concurrent writers or asynchronous VM failures. Work
counts describe logical operations, not a fixed total, byte count or elapsed time.

## Space and algorithm choice

Every complete call allocates an O(n) primitive index lane. Numeric-key calls also
cache O(n) primitive long keys. Some kernels need further primitive workspace.
The IN_PLACE_MERGE and AMERICAN_FLAG names describe their index-lane kernels;
the complete API call still has the common permutation cost.

The registry includes educational quadratic algorithms as explicit choices.
It does not select a fastest algorithm automatically, and it does not alter the
default JDK sorting policy. Stable choices preserve equal-key occurrence order;
an unstable algorithm makes no such guarantee. The catalogue records each exact
variant, stability, bounds, memory and applicability.

## Reproduce the bounded qualification

From a Java 21 checkout, verify the canonical proof-tooling mirror:

~~~sh
SORT_RECIPE=m3/tooling/sort-recipes/m3-java-util-sorts-20261018
mvn -B -ntp -f "$SORT_RECIPE/pom.xml" verify

# EVIDENCE_DIR must be outside the checkout.
mvn -B -ntp -f "$SORT_RECIPE/pom.xml" compile exec:java \
  -DtargetRoot="$PWD" -DapplyMode=check \
  -DreceiptPath="$EVIDENCE_DIR/m3-sorts-check.tsv"

python3 m3/verification/sorting/verify.py --java-home "$JAVA_HOME"
~~~

The receiver checks every sealed postimage and the exact existing sorting/progress
dependencies, compiles the actual generated java.base additions with Java 21,
runs the contract suite in normal and interpreted modes, records the public API,
and measures an exploratory clone-plus-sort workload. Its files go under
build/m3-sorting-qualification by default.

The benchmark reports first-call time, a warmed median, per-thread CPU time and
allocation for 512-element workloads. It is a single-VM observation with setup
included, not JMH, a universal ranking or a production performance gate.

These gates qualify the bounded source packet. A rebuilt M3JDK21 image, the actual
product jtreg harness, the current ordered receiving stage and platform/GC/CDS
acceptance require separate executed receipts. This candidate does not advance
those stages.
