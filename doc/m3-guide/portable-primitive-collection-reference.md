<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Portable primitive collection contracts

[Guide](README.md) · [Package map](m3-packages.md#package-11) · [Source and member coverage](portable-primitive-collection-source-coverage.md) · [Coverage](coverage.md)

The isolated `m3/ports/collections` receiver provides exact-width mutable lists,
deques and a sorted min/max queue in `com.m3.util`. It retains primitive arrays,
without one collection-owned wrapper or node per entry. Its five public types and
one package-private helper are the complete six-file production package at
`c842fab8cf5b799257e3c9ea07f8baddde80db09`.

This reference covers every complete body, all 97 explicitly declared public
constructors/methods, enum constants and the implementation helpers that determine
their effects. It is a source reference, not a claim of executed correctness or
whole-repository completion. Builds, tests, examples, verifiers, recipes,
benchmarks and native/runtime qualification are **NOT_RUN in this documentation
pass**. Hosted CI is separate exact-commit evidence.

## Placement and exact source routing

| Complete source | Owned contract | Explicit public callables |
| --- | --- | ---: |
| [M3PrimitiveKind.java][kind] | [Carrier model and shared storage](#carrier-model-and-shared-storage) | 2, plus eight enum constants |
| [M3DenseHash.java][hash] | [Package-private hashing support](#package-private-hashing-support) | 0 |
| [M3PrimitiveArrayList.java][list] | [Indexed list](#indexed-list) | 25 |
| [M3PrimitiveArrayDeque.java][deque] | [Typed ring deque](#typed-ring-deque) | 26 |
| [M3IntArrayDeque.java][intdeque] | [Specialized int deque](#specialized-int-deque) | 22 |
| [M3PrimitiveMinMaxQueue.java][queue] | [Sorted min/max queue](#sorted-minmax-queue) | 22 |

These are not the `com.m3.collections` classes in `m3/collections`, and do not
implement JDK `List`, `Deque`, `Queue` or `Collection<Long>`. The independent
[primitive long sequence reference](primitive-sequence-reference.md) and
[linked-hash reference](collections-reference.md) retain that other package's
contracts. In particular, their interfaces, factories, default methods,
iterators, module descriptor and capacity policies do not carry over here.

The seventh production file in this receiver is `com.m3.ds.M3LongFenwickTree`.
Its complete arithmetic/ownership contract and build context already belong to
[indexed algorithms: PrefixZ and Fenwick](indexed-algorithm-reference.md).
It is outside the six-file `com.m3.util` package and is not counted again here.
There is no `package-info.java` at this physical package and no `module-info.java`
in the receiver's source tree. A Markdown reference does not close inline-package
or module-delivery qualification gaps.

[Receiver documentation][receiver], [public provenance][provenance] and
[recipe metadata][recipe] identify this as an Apache-2.0 renamed materialization.
Synexia remains the canonical reusable implementation/recipe owner; M3JDK21 owns
receiver verification and later runtime integration. This review uses the public
target bodies and its published metadata only. It does not import donor test
results or infer permission to read any other source. The existing [porting
invariant][policy] and [machine naming/receiving plan][mapping] remain authoritative:
String → arrays → collections → AST/compiler → remaining families, with String
qualification active. Documentation of this receiver advances no phase and does
not change public JDK API names or per-file OpenJDK/third-party rights.

## Carrier model and shared storage

`M3PrimitiveKind` fixes the physical array type for an owner's lifetime. Its
public `widthBytes()` returns the lane width below. `payloadBytes(int capacity)`
requires nonnegative capacity, otherwise `IllegalArgumentException`, and returns
`(long) widthBytes * capacity`. This arithmetic fits in long for every accepted
int capacity. It allocates nothing and does not prove that such an array is
allocatable.

| Kind | Retained/snapshot array | Width | Carrier written and read |
| --- | --- | ---: | --- |
| `BOOLEAN` | `byte[]` | 1 | Zero becomes 0; every nonzero long becomes 1. Incoming nonzero bytes are canonicalized to 1 |
| `BYTE` | `byte[]` | 1 | Java `(byte)` narrowing, returned sign-extended to long |
| `SHORT` | `short[]` | 2 | Java `(short)` narrowing, returned sign-extended to long |
| `CHAR` | `char[]` | 2 | Java `(char)` narrowing; unsigned UTF-16 code unit, returned as 0–65535 |
| `INT` | `int[]` | 4 | Java `(int)` narrowing, returned sign-extended to long |
| `LONG` | `long[]` | 8 | Full signed-long value |
| `FLOAT` | `float[]` | 4 | Low 32 bits interpreted by `Float.intBitsToFloat`; reads use `floatToRawIntBits` and unsigned widening to long |
| `DOUBLE` | `double[]` | 8 | All 64 bits interpreted by `Double.longBitsToDouble`; reads use `doubleToRawLongBits` |

BOOLEAN is neither `boolean[]` nor bit-packed. CHAR is one code unit, not Unicode
code-point validation. Numeric-looking FLOAT/DOUBLE carriers are encodings: a
FLOAT carrier `1L` does not mean the floating value `1.0f`. To encode that value,
use `Integer.toUnsignedLong(Float.floatToRawIntBits(1.0f))`; use
`Double.doubleToRawLongBits(1.0d)` for DOUBLE. These are unexecuted explanatory
expressions. All integral carrier patterns are accepted and narrowed; no primitive
value, including zero, is reserved for emptiness.

### Equality, order and enum behavior

Equality first normalizes to the selected lane. Floating equality uses
`floatToIntBits`/`doubleToLongBits`, canonicalizing NaNs while distinguishing
negative and positive zero. Searches/removal therefore match different NaN
encodings, but do not equate the two signed zeros. Physical lane reads/copies use
raw-bit operations rather than equality normalization. There is no separate
canonical-NaN rewrite on ordinary storage. The Java floating conversions remain
the actual implementation boundary, rather than an independently tested
all-platform bit-preservation guarantee.

Ordering uses the matching primitive comparator; BOOLEAN false precedes true,
CHAR uses unsigned char order, and floating comparisons use `Float.compare` /
`Double.compare`. Negative zero precedes positive zero; NaNs follow positive
infinity and compare equal. Sorting delegates to the matching primitive
`Arrays.sort` overload. No stable order among equal values or NaN payloads is
promised. Binary search delegates to `Arrays.binarySearch` with the narrowed or
floating-decoded key.

The eight named enum constants are declared in the table's order. Compiler-created
`values()` and `valueOf(String)` and inherited enum methods are ordinary Java enum
behavior, not extra collection APIs; they are excluded from the 97 explicit
callables. In particular, enum `compareTo` orders kind constants, not stored
values. Carrier comparators/converters are package-private, not public methods.

### Capacity, default values and ownership

The list, typed deque and min/max queue require nonnull kind before validating
capacity. Their one-argument constructors reserve eight lanes. Explicit zero
reserves one lane; a negative capacity throws `IllegalArgumentException`.
Construction returns an empty owner regardless of reservation. Java's zero-filled
unused array lanes are not logical entries or an API-level default return value.

Shared `checkedSize(size, extra)` rejects negative inputs and a sum beyond
`Integer.MAX_VALUE` with `OutOfMemoryError`, before allocation/structural edits.
For growth, `grownCapacity(current, required)` first rejects a negative current
or required capacity with `IllegalArgumentException`, even before its
sufficient-capacity fast path. It returns current if sufficient; otherwise it computes `current + (current >>> 1) + 1` in long, takes at least the
required size and caps the growth preference at `Integer.MAX_VALUE - 8`.
A required size above that soft limit is returned directly. This is a soft growth
rule, not a hard VM array limit or successful-allocation guarantee. All retained
and temporary allocations may fail with `OutOfMemoryError` much earlier.
Only the list exposes public `ensureCapacity` and `trimToSize`.

Each typed owner stores a final `kind`, one mutable array reference and an int
`size`; ring owners additionally store int `head`. The specialized deque stores
`int[] elements`, `size` and `head`. No input array or callback is retained by
public import/traversal methods. Returned snapshots are new exact-size arrays;
later mutations in either direction do not alias. A package-private
`backingArray()` exists on the list and typed deque for same-package use; it is
not a public export contract. Their bulk methods explicitly defend self-aliases.
Callers must keep source arrays stable during copying; copying does not provide a
coherent snapshot against concurrent source mutation.

`payloadBytes()` counts reserved lanes, including unused capacity, times width.
It excludes owner/array headers, alignment, callback objects, temporary masks,
merge/copy arrays and JVM/process/native memory. None of these classes implements
an admission budget, `close`, secure erasure or automatic shrinking. Clearing
zeroes live lanes and keeps capacity. No wrapper-per-entry retention does not mean
zero allocation across all operations.

All four mutable owners are single-threaded. They have no locks, volatile
publication, versioned iterator, spliterator, stream, serialization, cancellation
or interrupt protocol. External coordination is needed for coherent shared
mutation/read access. Callback-based traversal is live and must not mutate its
owner. No concurrent snapshot, fail-fast or linearizability guarantee follows
from bounds checks or a private helper.

### Complete non-public kind helper responsibilities

These implementation atoms explain the public effects; they are not additional
public entry points:

| Helper group | Implemented behavior and caller boundary |
| --- | --- |
| Enum constructor and `widthBytes` field | Record each constant's physical width |
| `newArray`, `copyOf`, `clear` | Allocate, copy or zero-fill matching primitive arrays; `newArray` rejects negative length with `NegativeArraySizeException`; typed copying/filling retains JDK array validation/allocation behavior |
| `checkArray`, `checkRange` | Kind mismatch is `IllegalArgumentException`; `checkRange` first requires nonnull source, then matching kind, then `Objects.checkFromIndexSize` against reflected array length. Wrong-kind empty ranges are still rejected |
| `readBits`, `writeBits`, `storageBits` | Lane conversion as above; `storageBits` models the write/read carrier but is not called by the four owner bodies |
| `normalizeBits`, `equalBits`, `compareBits`, `hashBits` | Equality/order/hash semantics above. `hashBits` selects the int mixer for BOOLEAN/BYTE/SHORT/CHAR/INT/FLOAT and long mixer for LONG/DOUBLE; the four public containers do not use hashing |
| `sort`, `binarySearch` | Exact matching primitive `Arrays` overload, with BOOLEAN key canonicalization |
| `checkedSize`, `grownCapacity` | Checked logical size and overflow-safe growth preference described above |
| `ringIndex` | Map a legal logical offset by comparing with `capacity - head`, avoiding overflowing `head + logical`; owners supply valid ring coordinates |
| `copyRing`, `clearRing` | Up to two contiguous array copies or typed fills across wrap |
| `moveRing` | Overlap-aware logical movement; copy forwards for a lower destination and backwards for a higher destination, chunking at physical wrap boundaries. Owners validate ranges |
| `copyStorage` | Exact-width `System.arraycopy` for non-BOOLEAN; BOOLEAN canonicalizing loop with reverse traversal for destructive right-overlap |

## Package-private hashing support

`M3DenseHash` is final and package-private, with a private empty constructor.
It declares `DEFAULT_LOAD_FACTOR = 0.65f` and `MIN_CAPACITY = 8`; no member is
public. No hash map/set is implemented by this package.

`capacityFor(expectedSize, loadFactor)` first rejects negative expected size,
then requires a float strictly between 0 and 1. The comparison also rejects NaN
and infinities. It computes `max(8, ceil(expectedSize / (double) loadFactor))`,
rejects a requirement above `2^30` with `IllegalArgumentException`, and rounds
up to a power of two. `threshold(capacity, loadFactor)` directly evaluates
`min(capacity - 1, max(1, (int) (capacity * loadFactor)))`; it supplies no
independent argument validation and should not be generalized into a validated
public capacity API.

`mix(int)` uses unsigned shifts/xor and wrapping multiplication by
`0x7feb352d` and `0x846ca68b`. `mix(long)` uses wrapping long multiplication by
`0xbf58476d1ce4e5b9L` and `0x94d049bb133111ebL`, then folds high/low halves to
int. These deterministic avalanche mixers are not cryptographic or collision-free.
Only `M3PrimitiveKind.hashBits` calls them in these six bodies; the inspected
list/deque/queue owners do not call `hashBits`, `capacityFor` or `threshold`.
The presence of a helper does not establish an externally exposed hashing API or
execution of its paths by the five tests below.

## Indexed list

`M3PrimitiveArrayList` retains logical values in `[0,size)` of its linear array.
Simple single-element insertion/replacement/removal uses no new array when
retained capacity suffices. Bulk self-alias copies and predicate removal can
allocate temporary arrays; delegated sorting allocation is not bounded here.
The kind's conversion/equality rules always apply; return values are listed below.

### Complete public surface

| Entry point | Contract |
| --- | --- |
| `M3PrimitiveArrayList(kind)`; `M3PrimitiveArrayList(kind, initialCapacity)` | Empty construction with the shared kind/null/default/zero/negative/allocation rules |
| `kind()`; `size()`; `isEmpty()`; `capacity()`; `payloadBytes()` | Fixed kind; logical count; count-is-zero; reserved lane count; reserved payload only. All O(1) |
| `addBits(long bits)` | Append narrowed/decoded value after checked growth; amortized O(1) |
| `addBits(int index, long bits)` | Insert at any position `[0,size]`; invalid position throws `IndexOutOfBoundsException` before growth. Move suffix once, then write and increase size |
| `getBits(int index)` | Return stored carrier at `[0,size)`; otherwise `IndexOutOfBoundsException` |
| `setBits(int index, long bits)` | Same bounds; return previous carrier and replace lane without changing size/capacity |
| `removeAtBits(int index)` | Same bounds; return removed carrier, move suffix left and clear vacated final lane |
| `removeBits(long bits)` | Remove the first kind-equal value, returning true; false if absent. This is value removal, not indexed removal |
| `indexOfBits(long bits)` | First matching index, or `-1`; linear equality scan |
| `sort()` | Ascending primitive-array sort of live range only; mutates order, not size/capacity |
| `binarySearchBits(long bits)` | Requires compatible ascending order; result undefined if unsorted. Return a matching index, or `-(insertionPoint + 1)`; no first-duplicate guarantee |
| `clear()` | Zero live lanes and set size to zero; retain capacity |
| `ensureCapacity(int required)` | Negative request throws `IllegalArgumentException`; otherwise retain values/size and reserve at least required using shared growth when needed |
| `trimToSize()` | Copy to `max(1,size)` if different from capacity; can allocate/fail even though shrinking |
| `toPrimitiveArray()` | New independent matching primitive array of exactly size lanes in encounter order, including a zero-length array when empty |
| `forEachBits(LongConsumer action)` | Require nonnull even when empty; visit carriers in index order without snapshot; propagate callback exceptions and stop |
| `addAllBits(Object source, int from, int length)` | Append copied slice by delegating to indexed overload at current size; return length |
| `addAllBits(int index, Object source, int from, int length)` | Insert copied slice at `[0,size]`; validate index, nonnull/matching source, full slice and checked resulting size before mutation; return length |
| `removeRange(int fromIndex, int toIndex)` | Require `0 <= fromIndex <= toIndex <= size`, otherwise `IndexOutOfBoundsException`; half-open removal, one suffix move, clear vacated lanes. Empty valid range is a no-op |
| `removeIfBits(LongPredicate filter)` | Require nonnull even when empty; evaluate original carriers before method-owned compaction; return whether any matched. Detailed callback/failure contract below |

The private `check` implements `[0,size)` validation. Private `ensure` delegates
to `grownCapacity` then assigns a successful `kind.copyOf`. Indexed get/set are
O(1); arbitrary insertion/removal, searches, bulk movement, clear, traversal and
snapshots are O(n) in their affected/live ranges. Binary search is O(log(n+1));
comparison sorting has the primitive `Arrays.sort` implementation costs, with an
O(n log n) comparison-sort bound and kind-specific implementations. No workload
measurement or universal allocation bound is implied by this summary.

### Bulk insertion and predicate removal

Bulk insertion validates even a zero-length slice and a wrong-kind empty array.
An invalid destination index wins over source null/type/range validation. After
validation, zero length returns 0 without copying. If `source == elements`, it
allocates and fills an independent length-sized snapshot before growth or suffix
movement, then copies from that snapshot. Otherwise the caller array is copied
directly and never retained. BOOLEAN bytes normalize to 0/1. Growth can allocate a
second array. `size` is assigned after the suffix and incoming copies complete.
Input validation and required temporary/growth allocation precede logical edits;
this is not a general transaction against asynchronous failures or prohibited
concurrent/callback interference.

`removeIfBits` stores `originalSize`, reads each carrier, calls the predicate once,
and immediately compares current size with that original size. A detected size
change throws `ConcurrentModificationException`. It is a best-effort check:
`setBits`, sort, capacity changes, or a remove/add sequence restoring size are not
detected merely by this comparison. Callback mutation is prohibited even if it
escapes detection; the method neither restores callback changes nor guarantees
safe traversal after them.

No-match traversal allocates no decision mask. The first match lazily allocates
`ceil(originalSize/64)` longs. Predicate exceptions, detected size change and mask
allocation failure occur before this method's own compaction; any callback effects
remain. Once all decisions succeed, runs of survivors are copied in order with
raw lane copies, the vacated suffix is cleared, and size is published. An empty
list with a nonnull filter returns false. The decision mask is temporary, not part
of `payloadBytes`; total traversal/compaction is O(n). Ordinary `forEachBits`
supplies no corresponding size guard and no rollback of callback effects.

## Typed ring deque

`M3PrimitiveArrayDeque` stores encounter order in one arbitrary-capacity ring.
Head/tail insertion is amortized O(1); inspection and end removal are O(1).
The legacy `peek` names **throw on empty**, rather than returning null or zero.

### Complete public surface

| Entry point | Contract |
| --- | --- |
| `M3PrimitiveArrayDeque(kind)`; `M3PrimitiveArrayDeque(kind, initialCapacity)` | Shared typed-owner construction rules; default eight, zero becomes one |
| `kind()`; `size()`; `isEmpty()`; `capacity()`; `payloadBytes()` | O(1) kind, logical count/emptiness, reserved lanes and reserved payload |
| `addFirstBits(long bits)`; `addLastBits(long bits)` | Checked growth, then write at head/tail and increase size; carrier conversion applies |
| `peekFirstBits()`; `peekLastBits()` | Read corresponding end, or throw `NoSuchElementException` when empty |
| `getFirstBits()`; `getLastBits()` | Exact throwing aliases for corresponding peeks |
| `elementBits()` | Alias of `getFirstBits`, same empty exception |
| `removeFirstBits()`; `removeLastBits()` | Return corresponding end carrier and clear its lane; throw `NoSuchElementException` when empty |
| `pushBits(long bits)`; `popBits()` | Head-stack aliases of `addFirstBits` and `removeFirstBits` |
| `getBits(int index)` | O(1) logical encounter-order read at `[0,size)`; otherwise `IndexOutOfBoundsException` |
| `containsBits(long bits)` | Linear kind-equality search; return false if absent |
| `removeFirstOccurrenceBits(long bits)`; `removeLastOccurrenceBits(long bits)` | Scan from selected end for kind-equality; remove one match and return true, or return false |
| `clear()` | Up to two typed fills over live lanes, then zero size/head; retain capacity |
| `toPrimitiveArray()` | Independent matching array of exactly size lanes in encounter order, copied in at most two spans |
| `addAllBits(Object source, int from, int length)` | Validate nonnull, matching kind, range and checked resulting size; copy slice to tail; return length |
| `forEachBits(LongConsumer action)` | Require nonnull even when empty; live encounter-order traversal, callback must not mutate owner; exception propagates and stops |

Private `physicalIndex` uses `ringIndex`; `inc`/`dec` wrap one position without
modulo overflow. Private `ensure()` computes checked size plus one and delegates
to `ensure(required)`. Growth allocates the shared preferred capacity, copies
logical order into the new array in at most two spans, then assigns storage and
resets head to zero. Successful removal of the final element also resets head.
There is no public reservation/trim operation.

Occurrence removal's private `removeAt` compares prefix length `index` with suffix
length `size-index-1`, moves the shorter side and clears one abandoned lane.
On ties it moves the suffix. Prefix movement advances head; all empty results
reset it. Search plus movement is O(n), without per-entry allocation. Snapshots,
clear and traversal are O(n); an ordinary callback can allocate independently.

Bulk append rejects wrong-kind arrays even for an empty range. After validation,
zero length returns 0. A backing-array self-alias is snapshotted before growth or
copying. Append then uses at most two `copyStorage` spans, with per-byte BOOLEAN
normalization rather than a raw bulk copy, and publishes size last. Source arrays
are not retained. An alias snapshot and a growth array can both be needed.
There is no fail-fast version protocol for traversal, and callback errors do not
undo earlier callback effects.

## Specialized int deque

`M3IntArrayDeque` is a separate owner using a power-of-two `int[]` and masked ring
indices. Every int is valid data. Its capacity/null behavior deliberately differs
from the typed deque; do not infer one class's validation from the other's name.

### Complete public surface

| Entry point | Contract |
| --- | --- |
| `M3IntArrayDeque()` | Construct empty with eight lanes |
| `M3IntArrayDeque(int expected)` | Round up to a power of two with minimum two; every request below two, including negative values, becomes two. Requests above `2^30` throw `IllegalArgumentException` |
| `size()`; `isEmpty()`; `capacity()`; `payloadBytes()` | Logical count/emptiness, power-of-two reserved lanes, and `4L * capacity`; O(1) |
| `addFirst(int value)`; `addLast(int value)` | Insert at selected end, growing if needed; limit refusal is `IllegalStateException`, allocation failure can be `OutOfMemoryError` |
| `peekFirst()`; `peekLast()` | Read end or throw `NoSuchElementException` when empty |
| `getFirst()`; `getLast()`; `element()` | Throwing aliases; `element` reads head |
| `removeFirst()`; `removeLast()` | Return and zero the selected lane, or throw on empty |
| `remove()`; `pop()` | Head-removal aliases of `removeFirst` |
| `push(int value)` | Head-insertion alias of `addFirst` |
| `clear()` | Zero live ring in two fill calls, reset size/head, retain capacity |
| `forEach(IntConsumer action)` | Live head-to-tail traversal; null is a no-op when empty, and throws `NullPointerException` only if a value is visited. Callback errors stop traversal; mutation is prohibited |
| `addAll(int[] source, int from, int length)` | Require nonnull, validate full slice, check resulting capacity; copy to tail and return length |
| `toArray()` | New independent `int[size]` in encounter order, at most two copies |

Package-private `initialCapacity(expected)` rejects values above `2^30`, takes
`max(2,expected)`, and uses `highestOneBit(needed-1) << 1`.
`requiredCapacity(size,extra)` rejects negative inputs or a sum beyond `2^30`
with `IllegalStateException` before a shift/allocation. Under legal owner state,
all masked `head + size` calculations fit signed int at that limit. These helper
limits do not guarantee allocation success. Private `ensure` allocates the next
power-of-two capacity and copies encounter order via `copyInto` before publishing
the new storage and resetting head. `require` supplies the empty exception.

Bulk validation precedes zero-length return; null still fails on empty input.
Self-alias uses `Arrays.copyOfRange` before growth, then up to two array copies
append the slice and size is published last. The caller array is not retained.
Unlike the typed deque, ordinary final end removal need not reset head to zero;
logical emptiness and subsequent masked insertion remain valid. `clear` does
reset head. There is no public indexed access, occurrence-removal, kind query,
capacity reservation or trim API.

End operations are O(1), amortized for insertion; growth, clear, snapshots and
traversal are O(n), and bulk copying is O(m) plus any growth copy. Ordinary
traversal has no null precheck or fail-fast protocol. Prohibited callback mutation
may affect which values the live loop visits; it is not a supported iteration mode.

## Sorted min/max queue

`M3PrimitiveMinMaxQueue` is one logically ascending primitive ring, not a heap.
It retains duplicates and offers both extrema in constant time. Its insertion
trade-off is binary search plus movement, rather than heap-style logarithmic
insertion. Neither `peek` nor `poll` reserves an empty sentinel: all extrema
operations throw `NoSuchElementException` when empty.

### Complete public surface

| Entry point | Contract |
| --- | --- |
| `M3PrimitiveMinMaxQueue(kind)`; `M3PrimitiveMinMaxQueue(kind, initialCapacity)` | Shared typed-owner construction rules; default eight, zero becomes one |
| `kind()`; `size()`; `isEmpty()`; `capacity()`; `payloadBytes()` | O(1) fixed kind, count/emptiness, reserved lanes and retained payload |
| `offerBits(long bits)` | Insert after comparator-equal values using upper bound, checked growth and shorter-side movement; duplicates retained |
| `offerAllBits(Object source, int from, int length)` | Copy and insert a slice; only incoming values are sorted when needed; return length. Legacy empty-range type exception described below |
| `offerAllSortedBits(Object source, int from, int length)` | Validate matching kind and ascending input, then copy/merge without sorting; return length |
| `peekMinBits()`; `peekMaxBits()` | Return corresponding extremum without removal; throw on empty |
| `getMinBits()`; `getMaxBits()` | Exact throwing aliases of corresponding peeks |
| `pollMinBits()`; `pollMaxBits()` | Return and clear one end lane in O(1); throw on empty |
| `removeMinBits()`; `removeMaxBits()` | Exact throwing aliases of corresponding polls |
| `removeOneBits(long bits)` | Find lower bound, verify kind equality, remove that one value and return true; false if absent |
| `countBits(long bits)` | `upperBound - lowerBound`, using two binary searches; zero if absent |
| `clear()` | Clear live ring lanes, zero size/head and retain capacity |
| `toPrimitiveArray()` | Independent ascending matching primitive array of exactly size lanes |

Private `bound` binary-searches logical ring positions using the kind comparator.
`lowerBound` selects the first not-less position; `upperBound` selects the first
greater position. `physical` uses overflow-safe ring indexing. `offerBits` first
computes upper bound, then grows if needed, moves a shorter prefix or suffix and
writes the new carrier. Equal-length sides choose the suffix. `removeOneBits`
uses analogous shorter-side movement, clearing the abandoned lane. Polls and
successful removals reset head when the queue becomes empty. `requireNotEmpty`
implements the shared throwing-empty contract.

`countBits` is O(log(n+1)). A single offer/removal has O(log(n+1)) search plus
O(n) worst-case movement (and O(n) possible growth for insertion). Extrema
inspection/removal is O(1); clear and snapshot export are O(n). No iterator,
traversal callback, public indexed access, trim or reservation method exists.

### Bulk validation order and merge allocation

The two bulk methods intentionally differ:

1. `offerAllBits` calls `checkLegacyRange`: require nonnull source, obtain its
   length with reflection (non-array gives `IllegalArgumentException`), then
   validate `from` and `length` (`IndexOutOfBoundsException` on invalid range).
   A valid zero-length slice returns 0 **before matching-kind validation**.
   Thus an empty valid slice of another primitive array, an object array or
   another array type is accepted. Nonempty wrong-kind input is rejected with
   `IllegalArgumentException`, then checked resulting size is computed
2. `offerAllSortedBits` uses strict `kind.checkRange`, so null, wrong kind or
   invalid slice fail in that order even when length is zero. It checks the
   resulting logical size, returns 0 for empty input, and otherwise verifies
   every adjacent pair through `sortedRange`. Descending input throws
   `IllegalArgumentException` before retained state is changed. Equal values
   are valid; BOOLEAN input order is compared after zero/nonzero conversion

For ordinary bulk input of length one, `offerAllBits` reads that carrier then
uses `offerBits`. For an empty owner with longer input, it grows if necessary,
copies directly into retained storage at head zero, scans for sortedness and
sorts the retained incoming run only if needed, then publishes size. No separate
incoming temporary is allocated on that empty-owner path. For sorted input,
the empty-owner direct-copy path precedes its one-value fallback; a nonempty
owner with one incoming value uses `offerBits`.

For a nonempty owner and at least two incoming values, both methods allocate
an O(m) matching primitive temporary and copy/BOOLEAN-normalize into it before
queue mutation. Ordinary input is checked for sortedness and sorted there only
if needed. `merge` then grows if required and merges backwards into the retained
ring, reading the greatest remaining old/new values and writing the greatest
remaining destination. On comparator equality it selects the incoming value
for that backwards slot. It publishes size after the merge; the existing run is
never independently re-sorted. No stable NaN-payload tie order across all paths
is promised. Public APIs do not expose the retained queue array, and caller
input is not modified or retained.

For n existing and m incoming values, unsorted-input bulk work is
O(n + m log m), or O(n + m) when input is already sorted, including the scan
for order. The empty-owner path has the corresponding incoming-only cost.
Temporary storage for a nonempty multi-value merge is O(m), plus any newly
allocated retained growth array; old and new retained arrays can overlap in
lifetime. Input validation, ordering rejection and temporary allocation precede
method-owned logical changes. No concurrent transaction, arbitrary-Error rollback
or total-memory bound is supplied.

## Tests, callers and build evidence

### Five actual package tests

The complete [M3PrimitiveCollectionFacadesTest.java][tests] contains exactly five
`@Test` methods. These are the known external callers inspected for this package;
internal call sites are the owner/helper paths described above. This is not a
repository-wide production caller census. Assertions were read, not run.

| Test method | Assertions actually in the body | Boundary |
| --- | --- | --- |
| `primitiveKindsPreserveLaneWidths` | Eight enum widths and INT `payloadBytes(7) == 28` | No negative-capacity, conversion, equality/hash or floating tests |
| `primitiveArrayListPreservesExactWidthOwnerContract` | INT construction/growth, append/indexed insert, snapshot order, get/set old value, value removal, sliced append, sort/search hit, odd-value predicate removal, range removal, trim, size/nonempty | No failure callbacks, same-size mutation, alias paths, empty/invalid-range matrix or all-lane tests |
| `primitiveArrayDequePreservesLogicalOrderAndThrowingEmptyContract` | INT head/tail order, end/indexed reads, contains/first occurrence removal, head-stack aliases, traversal order, end removal and one empty `peekFirstBits` exception | No bulk append, last occurrence, snapshot, null traversal or all empty aliases |
| `intArrayDequePreservesPrimitiveOwnerContract` | Growth, sliced bulk append, snapshot, head-stack/queue aliases, end removal/read, clear/empty and one empty `getFirst` exception | No negative constructor, power-of-two boundary/overflow, self-alias or empty-null traversal |
| `primitiveMinMaxQueuePreservesSortedExtremaContract` | INT offers/duplicates, extrema, count, one-value removal, polls, sorted snapshot, clear/empty and one empty `getMinBits` exception | Neither bulk method is called; no sortedness/type/refusal/merge cases |

Additional unestablished behavior includes all-lane coercion/NaNs/signed zeros,
BOOLEAN normalization, raw-bit survival under moves/sort, randomized ring-wrap
oracles, allocation-failure boundaries, predicate-mask failure, capacity overflow,
null/type/range exception precedence, every alias and same-package backing-array
interference. `M3DenseHash` has no direct test in this file. Source/member closure
does not fill these test gaps; donor receipts are not substituted for them.

### Independent build route

The [receiver POM][pom] declares
`com.m3.compat:m3-collection-port-wave1:0.1.0-SNAPSHOT` and Java release 21.
Compiler plugin `3.13.0` supplies `-Xlint:all` and `-Werror`; Surefire `3.5.2`
sets `useModulePath=false`. The exact test dependency is
`org.junit.jupiter:junit-jupiter:5.11.4-m3-source-1`, because the `5.11.4`
property is suffixed in the dependency declaration. It is not stock JUnit
`5.11.4`, and standalone artifact/classpath resolution was not tested here.

Profiles `m3-synexia-atom-pattern` and `m3-synexia-atom-pattern-mastery` have no
default activation. They configure separate OpenRewrite `dryRun` tooling at
verify; their recipe dependency is not a runtime dependency of these owners.
The [m3 control reactor][reactor] does not list `ports/collections` among its
default or explicit module-addition profiles. Its `m3-extended` profile adds
`collections`, the different project. General reactor execution must not be
presented as this receiver's verified route.

The [collection-port workflow][workflow] declares pull-request path triggers for
`m3/ports/collections/**` and itself, plus manual dispatch. It sets up Java 21,
runs diff hygiene, invokes the provenance verifier, then declares
`mvn -B -ntp -f m3/ports/collections/pom.xml clean test`. This command is described
from source and **NOT_RUN here**. Changes confined to `doc/m3-guide/**` do not
match those pull-request paths; a green documentation-related workflow is not
proof that this contract suite ran. Exact-commit hosted results need separate
inspection rather than inference from the workflow declaration.

### Provenance gate and remaining qualification

The [public provenance rows][provenance] declare donor commit
`8513abf8eb0d911873c9127a138007a42dd4ab60`, source paths/blobs, Apache-2.0 and
renaming dispositions. The [donor-review table][donorreview] marks other library
candidates as evidence-only pending exact license/revision review, and challenge
sites as taxonomy only. It does not authorize importing those implementations.

The [verifier][verifier] checks provenance schema, duplicate target rows,
repository/commit/license fields, 40-hex donor-blob strings, the expected seven
production rows, donor-header substrings, target package text, forbidden
`import com.synexia` / `package com.synexia` text and recipe fields. It does not
recompute current target implementation hashes against donor bodies, compile,
execute behavior, prove a complete import graph, or authenticate runtime
acceptance. The source's printed PASS token is not a result from this review.
Target Git blobs in the [coverage companion](portable-primitive-collection-source-coverage.md)
identify inspected M3 bytes; `Source-Git-blob` header values identify declared
donor bytes and must not be substituted for target hashes.

This closes source-level routing for these six production files only. Inline
package documentation, exhaustive repository-wide callers, dependency resolution,
actual target tests, compiler/recipe/refusal execution, resource measurements and
JDK/native/runtime qualification remain open. No generated custody inventory,
sealed recipe payload, build/workflow configuration or receiving status is changed.

[kind]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveKind.java
[hash]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3DenseHash.java
[list]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java
[deque]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java
[intdeque]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java
[queue]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java
[receiver]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/README.md
[provenance]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/PROVENANCE.tsv
[recipe]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/recipe.json
[tests]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/test/java/com/m3/util/M3PrimitiveCollectionFacadesTest.java
[pom]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/pom.xml
[workflow]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/.github/workflows/m3-collection-port-wave1.yml
[donorreview]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/DONOR_REVIEW.tsv
[verifier]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/verify_collection_port_wave1.py
[policy]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/docs/m3jdk21-porting-invariant.md
[mapping]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/docs/name-mapping.json
[reactor]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/pom.xml
