<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Joined-array and immutable-view reference

[Guide](README.md) · [Package map](m3-packages.md#package-2) · [Indexed-string reference](indexed-string-reference.md) · [Runtime owners](runtime-owners.md) · [Coverage](coverage.md)

`com.m3.arrays` defines separately built byte, UTF-16, int and long views.
Its factory copies incoming mutable arrays; slices and joins retain references
to views; explicit export produces ordinary mutable arrays. These are Java
objects describing logical sequences, not new VM array types, a global interner,
or a way to pass segmented Java storage to JNI as one contiguous array.

This reference reviews all seven public source owners, their private factory
implementations, both Java test classes, the native bridge and its small C test
at public source revision `1eac151fe56bccdc33b24a0155cc1ac5b9959005`, inspected
on 14 October 2026. The method descriptions below are body-derived contracts,
with current source gaps called out before use. No compiler, Maven build, CMake,
CTest, JNI test, jtreg, recipe/policy verifier or benchmark was run. Examples,
tests and build routes are **NOT_RUN**. Source/hash review is not successful
module qualification, installed JDK delivery, or measured performance evidence.

## 1. Current source gaps and acceptance boundary

Two issues prevent treating the source descriptions as a qualified implementation:

1. **Missing snapshot method implementations.**
   [`M3ByteArrayView.segmentCount()`][byte-count] and
   [`M3Utf16ArrayView.segmentCount()`][char-count] are abstract interface methods.
   The private final [`ByteSnapshot`][byte-snapshot] and
   [`CharSnapshot`][char-snapshot] classes do not declare this method. Neither
   class has a superclass other than `Object`; neither interface supplies a
   default, and inspected `Object`/`CharSequence` bodies supply none. This is a
   source-observed implementation obligation left unsatisfied. It is not a
   reported compiler run or test failure, and this page does not invent the
   missing implementation or silently promise a snapshot segment count.
2. **Empty children can break indexed join navigation.** The factory preserves
   empty input views and builds cumulative ends, then `locate` uses
   `Arrays.binarySearch(ends, index + 1)`. An exact match among duplicate ends
   need not choose the first matching child. The concrete trace below selects
   an empty child and an invalid local index. Export through byte/char buffers
   takes another path, so a passing-looking copy assertion would not establish
   indexed lookup correctness.

### Exact empty-child trace, without execution

Consider the permitted int-view construction:

```java
M3IntArrayView joined = M3Arrays.join(
        M3Arrays.ints(new int[] {10}),
        M3Arrays.ints(new int[0]),
        M3Arrays.ints(new int[] {20}));
// Inspect joined.intAt(0); this example has not been compiled or run.
```

[`checked`][join-check] retains all three children. [`IntJoin`][int-join]
computes `ends = [1, 1, 2]`, total length 2. Its index check accepts 0.
[`locate`][locate] searches for 1. The pinned
[`Arrays.binarySearch0`][binary-search] starts at low 0/high 2,
chooses middle 1 and returns that exact match. `IntJoin` therefore uses
`previous = ends[0] = 1` and invokes the empty child's `intAt(-1)`, whose
bounds check rejects it. This is a deterministic source trace for that layout
and this pinned binary-search body, not a claim that every empty join fails.
The public binary-search contract also makes no promise to select the first
of duplicate matches. All four join implementations use this locator;
int/long `copy()` and UTF-16 search/streams also depend on indexed access.

The module [README][readme] describes the intended bounded receiving slice.
The [porting invariant][invariant] and [machine receiving sequence][mapping]
remain the authorities: String qualification precedes arrays, then collections,
AST/compiler and remaining families. This documentation does not advance a
phase. Fixing source or its sealed recipe is a separate qualified change.

## 2. Public types, ownership and compatibility

The [descriptor][module] exports `com.m3.arrays` from module `com.m3.arrays`;
it declares no additional required module beyond implicit `java.base`.

| Public owner | Declared role |
| --- | --- |
| [`M3Arrays`][factory] | Final static factory/composition class, private constructor; five snapshot overloads and four typed `join` overloads |
| [`M3ByteArrayView`][byte] | Byte length/index/slice, segment descriptors and explicit copy defaults |
| [`M3Utf16ArrayView`][chars] | Extends `CharSequence`; UTF-16 code-unit access, facts, search, comparison, traversal and export |
| [`M3IntArrayView`][ints] | Int length/index/slice and copy defaults |
| [`M3LongArrayView`][longs] | Long length/index/slice and copy defaults |
| [`M3Utf16Facts`][facts] | Public record of eight composable scalar facts, checked constructor, `empty`, `of` and `combine` |
| [`M3ArrayNative`][native] | Final static explicit library loading, UTF-16 comparison and array concatenation; private constructor |

These interfaces are open, not sealed. Factory snapshots own cloned/copied
primitive payloads, but `join` accepts arbitrary interface implementations
without verifying immutability, nonnegative/stable lengths, facts, descriptor
coverage or agreement between buffers and indexed reads. Its null checks do
not turn a mutable or dishonest custom implementation into an immutable owner.
Custom implementations must preserve those invariants for the derived views
and cached metadata to remain meaningful.

Factory objects do not expose a setter or backing mutable array. Returned
read-only NIO buffers reject content writes through that descriptor, but their
position/limit remain caller-local mutable cursor state. A read-only descriptor
alone cannot freeze a custom implementation's externally mutable storage.
Neither payload identity nor equal content is interned by these classes; two
snapshot calls copy independently. There is no OS-provided universal identity,
shared mapping, namespace/generation registry or cross-process deduplication
implemented here.

A slice retains its source and can keep an entire larger snapshot or join
reachable. A join retains all child views, including empty ones, and a private
copy of its reference array. Nothing closes, unmaps or explicitly releases a
view: ordinary Java reachability governs these heap owners. Buffer descriptors
also keep their storage reachable. There is no `AutoCloseable` or native-pointer
lifetime protocol in the view interfaces.

The view implementations do not override `equals` or `hashCode` to compare
payloads. `M3Utf16ArrayView.javaHashCode()` and `contentEquals(...)` are explicit
operations; they do not change `Object` identity-based equality/hash semantics.
`compareTo(M3Utf16ArrayView)` does not make the interface implement
`Comparable`. The UTF-16 private implementations override `toString()` to
materialize text; byte/int/long implementations retain the ordinary `Object`
form. `M3Utf16Facts`, as a record, has its own component-based equality/hash
and record diagnostic `toString`, distinct from the stored String hash.

The factory implementations retain immutable state after construction, but
admission/copy does not provide an atomic snapshot of concurrently modified
inputs. Keep source arrays, mutable `CharSequence`s, custom views, needles,
copy targets and JNI arrays stable while an operation uses them. No lock,
transaction, fail-fast mutation detection or mutable-source synchronization is
provided. Native loader synchronization only protects loader state.

## 3. Factory, join and range contracts

### Every `M3Arrays` factory overload

| Method | Source behavior and ownership |
| --- | --- |
| `bytes(byte[] source)` | Rejects null; clones the whole array, then retains the clone in `ByteSnapshot` |
| `chars(char[] source)` | Rejects null; clones the array; `CharSnapshot` computes facts over that private clone |
| `chars(CharSequence source)` | Rejects null; allocates a `char[]` using the initial length, copies each indexed code unit, then computes facts. No encoding/normalization. The source must remain stable during this copy; later changes do not affect the completed snapshot |
| `ints(int[] source)` | Rejects null; clones and retains a private `int[]` |
| `longs(long[] source)` | Rejects null; clones and retains a private `long[]` |
| `join(M3ByteArrayView... values)` | Validates the array and every child for null; returns the only child unchanged, creates an empty byte snapshot for zero children, otherwise constructs `ByteJoin` |
| `join(M3Utf16ArrayView... values)` | Same admission/identity cases; multi-child `CharJoin` also combines each child's stored facts |
| `join(M3IntArrayView... values)` | Same admission/identity cases, with `IntSnapshot`/`IntJoin` |
| `join(M3LongArrayView... values)` | Same admission/identity cases, with `LongSnapshot`/`LongJoin` |

Use a typed empty varargs array when selecting an empty join overload; the
four unrelated varargs signatures make an untyped `M3Arrays.join()` call
ambiguous. There is no `T[]`, object-array, float, double, short or boolean view
factory in this package. An existing UTF-16 view passed to `chars(CharSequence)`
is copied; use `join`/`subSequence` when the intended contract is retention.

Despite the local variable name `flattened`, `checked` only validates and copies
child references. It does not expand nested joins, drop empty children or
flatten payload. Each multi-child join then clones the reference array again
and constructs `int[] ends`. Total length is accumulated with
`Math.addExact(int,int)`, so representational overflow throws
`ArithmeticException`; it is not silently truncated to a larger logical array.
Custom negative lengths are not explicitly refused at this admission boundary.

Indexed access checks the logical index, binary-searches immediate child ends,
subtracts the preceding end and delegates to that child. Nested joins can add
further navigation levels. The source does not rebalance a tree or promise
constant-time access or bounded nesting; no measured complexity benefit is
established. See the duplicate-end issue above before relying on this path.

### Every length, indexed-read and range method

| Surface | Contract established by the factory implementations |
| --- | --- |
| All four `length()` methods | Primitive element count; UTF-16 length counts `char` code units, not Unicode code points or encoded bytes |
| `byteAt(int)`, `charAt(int)`, `intAt(int)`, `longAt(int)` | Accept `0 <= index < length`; use `Objects.checkIndex` and reject other indexes with `IndexOutOfBoundsException`; return the primitive value, with bytes retaining Java's signed byte representation |
| `M3ByteArrayView.slice(start,end)` | Half-open byte range; keeps source, start and length |
| `M3IntArrayView.slice(start,end)` / `M3LongArrayView.slice(start,end)` | Half-open element range; keeps source, start and length |
| `M3Utf16ArrayView.subSequence(start,end)` | Half-open UTF-16 code-unit range with covariant `M3Utf16ArrayView` return; computes new facts for a proper slice |

Ranges require `0 <= start <= end <= length`, using
`Objects.checkFromToIndex`; invalid ranges throw `IndexOutOfBoundsException`.
Empty ranges, including `[length,length)`, are allowed. A full-range request
returns `this`. A proper range makes a descriptor rather than copying payload.
Slicing an existing slice uses the original slice source plus adjusted bounds,
so consecutive slices do not add one new slice layer each time. Slicing a join
does not prune its retained child tree.

UTF-16 proper slices scan their exposed code units immediately to compute
`M3Utf16Facts`. Byte/int/long slices have no corresponding fact scan. Thus
"no payload copy" does not imply allocation-free or constant-time slicing.
A UTF-16 slice may start or finish within a surrogate pair; its facts and
code-point traversal apply only to the retained range, not outside context.

### Segment descriptors and counts

`M3ByteArrayView.segmentCount()` and `asReadOnlyBuffers()`, and
`M3Utf16ArrayView.segmentCount()` and `asReadOnlyCharBuffers()`, are the only
segment-export APIs here. Int/long views provide neither buffer arrays nor
segment counts.

- Snapshot buffer export returns a fresh empty descriptor array for empty
  payload, or a new one-element array wrapping the private payload read-only
- Byte/char join export enumerates every child's exported descriptors in order,
  omits descriptors with no remaining elements and makes read-only duplicates;
  nested descriptor lists are traversed, but payload is not concatenated
- Byte/char slice export walks the source's descriptor sequence, skips the
  prefix and creates read-only buffer slices for exactly the selected span.
  Buffer `remaining()` and `position()` define the described region. Original
  descriptor positions are not advanced
- Slice `segmentCount()` actually builds that trimmed descriptor list and
  returns its size. Join `segmentCount()` sums child counts with `addExact`,
  which can throw `ArithmeticException`; it does not count by exporting buffers
- Snapshot `segmentCount()` is missing in the inspected source. The source
  tests expect two segments for a two-nonempty-child char join and for a byte
  join containing a middle empty child; those assertions do not supply the
  missing implementation

Counts and buffer coverage are trusted for custom implementations. A slice
whose exported source regions do not cover its requested length throws
`IllegalStateException` with a byte/UTF-16 coverage message. No full consistency
scan compares buffer payloads with `byteAt`/`charAt`, and extra regions beyond
the requested range are not a global admission error. Buffer arrays and cursor
objects are allocations; read-only export is not a claim of zero allocation.

## 4. Copying and ordinary array materialization

Every view declares a same-primitive `copyTo(int sourceStart, T[] target,
int targetStart, int count)` default and `T[] copy()` default, where the actual
primitive is `byte`, `char`, `int` or `long`; there is no generic Java `T`
parameter in these signatures.

All four `copyTo` methods first reject a null target, then validate the source
range, then the target range using `Objects.checkFromIndexSize`. Negative starts
or counts and ranges extending beyond either length throw
`IndexOutOfBoundsException` before ordinary valid-view copying begins. A zero
count allows a start exactly at either length but still validates the arguments.
For byte/char, zero count does not guarantee that buffer-provider callbacks are
skipped: the default still obtains and traverses descriptors as needed.

| Export | Traversal and effects |
| --- | --- |
| Byte `copyTo` | Duplicates each source `ByteBuffer` read-only, uses its remaining span and indexed bulk `get` into the target; does not use `byteAt` |
| UTF-16 `copyTo` | Same algorithm with `CharBuffer`; copies exact code units, including isolated surrogates; does not use `charAt` |
| Int `copyTo` | Loops over `intAt(sourceStart + index)` and writes the target in order |
| Long `copyTo` | Loops over `longAt(sourceStart + index)` and writes the target in order |
| Each `copy()` | Allocates a fresh mutable array of `length()` and delegates full-range copying into it; empty results also use a new zero-length array expression |

For byte/char, null individual buffer descriptors are rejected. Too few
remaining elements produces `IllegalStateException` after any already copied
prefix; extra descriptors after satisfying the count are not validated. A
custom provider can also throw while enumerating/accessing buffers. Int/long
custom indexed reads can throw after earlier target writes. These methods do
not roll back a partially modified target or guarantee overlap-safe copying
for a custom view that exposes the same mutable array as the target. Factory
snapshots do not ordinarily expose such a mutable alias.

`copy()` is a new mutable value, so changing its result cannot mutate a valid
immutable factory owner. UTF-16 snapshot `toString()` calls `new String(data)`;
slice/join `toString()` call `new String(copy())`. The latter explicitly
materialize a temporary `char[]` before constructing the String. There is no
promise of interning or original String identity. Byte/int/long `copy()` is
not text decoding.

No view is itself a Java array. Its length, slicing or read-only buffers do not
change array identity, covariance, `ArrayStoreException`, reflection, clone,
serialization, Unsafe/VarHandle, GC barriers or JNI array rules. Object-array
adoption and mapped/shared owners remain separate qualification work.

## 5. UTF-16 facts, navigation, comparison and streams

### Every `M3Utf16Facts` member

The record constructor is
`M3Utf16Facts(int length, int javaHashCode, int hashPower31,
int codePointCount, boolean ascii, boolean latin1, char first, char last)`.
It rejects negative length, negative code-point count, or count greater than
length with `IllegalArgumentException`. It does **not** prove that the supplied
hash, power, character endpoints, flags or count describe any actual payload.
Callers can construct internally inconsistent facts; a view returning them is
not validated by join or `contentEquals`.

| Accessor/method | Meaning or operation |
| --- | --- |
| `length()` | Stored UTF-16 code-unit count |
| `javaHashCode()` | Stored Java String polynomial hash, not this record's `hashCode()` |
| `hashPower31()` | Stored power of 31 for this many code units, using Java `int` wraparound |
| `codePointCount()` | Stored count treating a high surrogate followed by a low surrogate as one point and each other code unit as one |
| `ascii()` | `of` sets true exactly when all code units are less than 128; true for empty input |
| `latin1()` | `of` sets true exactly when all code units are at most 255; true for empty input. This flag is not an encoding operation |
| `first()` / `last()` | Stored endpoint code units; `of` uses zero for both on empty input, so use `empty()` to distinguish an empty value from actual NUL endpoints |
| `empty()` | Tests stored `length == 0` |
| `of(CharSequence value)` | Rejects null; scans indexed UTF-16 units, calculating all eight fields without retaining the input. Hash is `31 * hash + c`; power starts at 1 and multiplies by 31. Input must stay stable throughout |
| `combine(M3Utf16Facts left, M3Utf16Facts right)` | Rejects null; returns the nonempty side's same record reference when the other is empty (both empty returns `right`). Otherwise checks length/count addition, composes hash/power, ANDs flags and uses left first/right last |

For nonempty inputs `combine` subtracts one from the summed code-point count
when left ends in a high surrogate and right starts in a low surrogate. Hash
composition is `leftHash * rightPower + rightHash`; power is the product.
Those hash/power operations deliberately wrap as Java `int`, while length and
initial point-count sums use `Math.addExact` and can throw
`ArithmeticException`. The record constructor then applies its limited checks;
forged facts can yield a rejected result or a plausible but incorrect result.
Facts combine fixed-size metadata; they do not read or own character payload.

`CharSnapshot` caches facts once, `CharSlice` caches a fresh scan and `CharJoin`
caches composition once. A valid join can therefore repair the count for a
surrogate pair across child seams without concatenating the character arrays.
Empty-fact identities let a seam survive an empty middle child at the fact
level; this does not repair the separate indexed-locator issue.

### Every remaining UTF-16 view operation

`length`, `charAt`, `subSequence`, `segmentCount`, buffer export and copying
are covered above. `facts()` returns the cached record in each factory-owned
UTF-16 implementation. Remaining methods are:

| Method | Exact behavior and boundary |
| --- | --- |
| `javaHashCode()` | Returns `facts().javaHashCode()`; no new text scan |
| `codePointCount()` | Returns `facts().codePointCount()` for the whole view |
| `codePointAt(int index)` | Checks a code-unit index in `[0,length)` then delegates to `Character.codePointAt(this,index)`. Combines a high/low pair if both units are inside this view, even across a physical seam |
| `codePointBefore(int index)` | Requires `0 < index <= length`, otherwise throws `IndexOutOfBoundsException`; delegates to `Character.codePointBefore` and can combine the preceding pair |
| `offsetByCodePoints(int index,int codePointOffset)` | Delegates to `Character.offsetByCodePoints`; accepts a starting boundary in `[0,length]`, moves forward/backward by code points and throws `IndexOutOfBoundsException` when the start or requested traversal is outside the view |
| `chars()` | Returns `IntStream.range(0,length()).map(this::charAt)`: sequential, lazy code-unit reads, one nonnegative `int` per UTF-16 unit. Captures the range length when called, not a defensive copy |
| `codePoints()` | Scans immediately into an `IntStream.Builder`, combining adjacent high/low pairs; builds and returns the stream only after that scan. It is eager preparation with buffered point values, not the lazy `chars()` path |
| `contentEquals(M3Utf16ArrayView other)` | Rejects null; refuses different lengths or stored Java hashes before comparing all code units. Equal hashes do not bypass exact comparison. Incorrect custom facts can cause false rejection |
| `compareTo(M3Utf16ArrayView other)` | Rejects null; lexicographic unsigned UTF-16-unit ordering. Returns the first `char` difference, or length difference after a common prefix; not locale, grapheme or code-point ordering |
| `indexOf(CharSequence needle,int fromIndex)` | Rejects null. Clamps negative start to zero. Empty needle returns `min(max(0,fromIndex),length)`. Otherwise scans candidate code-unit positions forward and returns first exact match or `-1` |
| `lastIndexOf(CharSequence needle,int fromIndex)` | Rejects null. Empty needle returns `-1` for negative start, otherwise `min(fromIndex,length)`. Nonempty needle starts at `min(fromIndex,length - needle.length())`, scans backward and returns the last exact match at/before that start or `-1` |

The inherited `CharSequence.isEmpty()` tests `length() == 0`; it is not a second
fact-based emptiness cache. The interface explicitly overrides the
`CharSequence` stream defaults. Stream values preserve unpaired surrogates as
individual code-unit values, rather than rejecting or replacing them. A low
surrogate index is still a legal `codePointAt` input; navigation does not insist
that callers begin at a Unicode scalar boundary. No grapheme segmentation,
normalization, case folding or regex behavior is implemented here.

Search does not build a prefix table, cache a needle, copy a mutable needle or
promise linear-time matching. It repeatedly calls `length`/`charAt` on the
needle and the view. Matches may cross child seams, contain isolated surrogates
or end within a surrogate pair. Keep a mutable needle unchanged throughout.

For example, the intended logical text from `{'A','\ud83d'}` joined with
`{'\ude00','B'}` has length 4 and code-point count 3. The seam is a pair at
logical indexes 1 and 2. Slicing `[2,4)` keeps a low surrogate plus `B`, so its
count is 2. This is a source-derived illustration, **NOT_RUN**; the source gap
in snapshot `segmentCount` remains a qualification blocker.

### Charset boundaries belong to a separate API

No method here accepts a `Charset`, encoder, decoder, malformed-input action or
unmappable-character action. A byte view is just bytes; a UTF-16 view is Java
`char` units. `copy`, `toString`, JNI compare and JNI concatenate do not perform
UTF-8/Latin-1/modified-UTF-8 encoding or Unicode normalization. `ascii`/`latin1`
facts do not transform storage.

A consumer performing encoding must define error policy and carry encoder
state over all logical segments; encoding each char buffer independently can
split a surrogate pair or reset a stateful charset. Reuse the distinct
[indexed-string encoding and I/O guide](indexed-string-reference.md#5-bytes-encoding-io-and-native-lifetime)
for its existing `M3Text` APIs and tests. Those APIs, charset tests, mapped
owners and interning guarantees are not methods or test coverage of this
`com.m3.arrays` package. The [runtime owner guide](runtime-owners.md) likewise
covers a separate Java/HotSpot String integration boundary.

## 6. Explicit JNI loading, fallback and contiguous output

### Every public `M3ArrayNative` operation

| Method | Behavior |
| --- | --- |
| `load(Path library)` | Static synchronized, no return value. If already loaded, returns immediately without checking even a null or different path. Otherwise rejects null, converts to absolute normalized path and calls `System.load`; sets the volatile flag only after successful return |
| `isLoaded()` | Reads that volatile flag; indicates that this class's explicit load call returned, not that both native symbols have been called, ABI parity has passed, or an OS/JDK-wide provider has been qualified |
| `compareUtf16(char[] left,int leftStart,char[] right,int rightStart,int count)` | Checks left then right for null, then both ranges. With flag false, compares exact `char` differences in Java; with flag true, calls the private JNI function. Returns first nonzero unit difference or zero for equal selected ranges, including zero count |
| `concatenateUtf16(char[]... segments)` | Rejects a null outer array and each null child; sums lengths with `Math.addExact(long,long)`. Total above `Integer.MAX_VALUE` throws `OutOfMemoryError`. With flag false allocates a fresh `char[]` and copies each child using `System.arraycopy`; with flag true calls native concatenation |

There is no auto-loading, provider search, unload, reset, close, or enable/disable
method. A never-loaded class uses Java directly. `load` does not swallow
`UnsatisfiedLinkError`, security/path failures or other exceptions; before the
first successful load its flag remains false when loading fails. Callers can
handle that failure and later use the Java branch, but the method itself does
not silently recover. Once the flag is true, native linkage or JNI failures
are not caught and retried in Java. A library that loads but lacks the expected
symbol can fail when the operation is first invoked. Loader state belongs to
the loaded `M3ArrayNative` class, not a universal cross-loader service.

Comparison validates ordinary array ranges even for zero count and uses
`Objects.checkFromIndexSize`, so invalid public ranges throw
`IndexOutOfBoundsException` before native dispatch. Equal-length comparison
can return zero for different array identities. It does not compare lengths
outside the caller's selected count or interpret surrogate pairs as scalars.
Concatenation preserves every code unit exactly, including isolated halves;
zero segments and all-empty segments still request an ordinary new empty array.
It does not retain input arrays or promise a consistent snapshot while other
threads mutate children or the varargs array. Actual allocation can fail below
the integer limit too.

### What the native implementation actually does

The [JNI bridge][jni] accepts ordinary `jcharArray`/`jobjectArray` parameters,
not view interfaces or buffer descriptor arrays. Comparison uses two local
1024-`jchar` chunks and `GetCharArrayRegion`, then calls the small
[`m3_compare_utf16`][native-c] loop. Concatenation first checks child lengths,
allocates one Java result with `NewCharArray`, and transfers chunks through one
1024-`jchar` local buffer using `GetCharArrayRegion` and `SetCharArrayRegion`.
It deletes child local references as it advances and retains no heap pointer
after return. It does not call `GetPrimitiveArrayCritical`, expose a direct
pointer, coalesce Java segments through virtual memory, or eliminate copying.

JNI's defensive null/range helper raises `IllegalArgumentException` unless an
exception is already pending. This is not the public Java wrapper's null/range
contract: the wrapper rejects those arguments first with its own checks.
Checked JNI region-operation failures return while preserving the pending
exception. Native total-length overflow raises `OutOfMemoryError` and returns
null; a failed `NewCharArray` returns null to the VM with the allocation failure.
There is no Java fallback catch around any of these native paths. The bare C
comparison function is not a bounds-checking Java API; callers provide valid
pointer ranges. Its nonzero-count null-pointer case returns zero and is not a
substitute for the JNI wrapper's validation.

For a segmented view requiring one `char[]`, `view.copy()` is the explicit
flattening step. `M3ArrayNative.concatenateUtf16` instead accepts already
ordinary arrays and creates another ordinary array. Neither call makes a
segmented view itself contiguous, and no view overload exists on this native
class. Keep all source units and charset policy distinct from byte encoding.

## 7. Build declarations and actual test coverage

The [POM][pom] declares `com.m3:m3-arrays:0.1.0-SNAPSHOT`, Java release 21,
compiler plugin 3.13.0 with `-Xlint:all,-module` and `-Werror`, and Surefire
3.2.5 with module-path tests disabled. Its test dependency is exactly
`org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1`; dependency availability was
not resolved here. `LICENSE`, `NOTICE` and `provenance.json` are packaged under
`META-INF`. Recipe profiles are authoring/proof tooling, not a Synexia runtime
dependency of these view classes.

The [module README][readme] supplies these routes, **not run in this pass**:

```sh
mvn -f m3/arrays/pom.xml test
mvn -f m3/arrays/pom.xml -Pnative-linux test
```

The `native-linux` profile invokes CMake configuration, build and CTest, and
sets `m3.arrays.native.path` to `target/native/libm3_arrays.so` for Surefire.
[CMake][cmake] requires version 3.20, JNI headers and C11 features; it builds
`m3_arrays` and, when testing is enabled, `m3_arrays_checks`. Warning-as-error
options are selected for MSVC versus other compilers. The Maven profile uses
Release configuration and a Linux `.so` path; this is not proof of portable
macOS/Windows execution. The C checks use `assert`; Release/NDEBUG behavior
must be considered when assessing whether those assertions actually execute.
The snapshot source omission above must also be resolved through its owning
change before assuming these build routes can qualify the current module.

### Every Java test method, inspected rather than executed

| Test in [`M3ArraysTest`][tests] | Assertions actually present |
| --- | --- |
| `snapshotsDetachFromMutableInputsAndExportsAreIndependent` | Mutates each primitive input after admission, compares copies for all four types; mutates exported byte/char arrays and checks original indexed values; rejects write-through byte/char buffer operations |
| `utf16JoinKeepsExactHashAndCodePointFactsAcrossSurrogateSeam` | Two-child split-surrogate join: length, String polynomial hash, point count, `codePointAt`, `codePoints`, expected segment count and `toString` |
| `slicesAndJoinsRemainCoordinateExactWithoutFlattening` | Three-char-child joined text, expected segment count, range text, within/across-seam search, empty-needle boundary cases and `copyTo` subrange |
| `primitiveJoinsPreserveOrderAndCheckedSlices` | Int/long join copies and slices, one past-end int read and one negative-start long slice |
| `byteJoinUsesScatterGatherReadOnlyDescriptors` | Nonempty/empty/nonempty byte join: logical length, expected count, whole copy, cross-seam slice copy and descriptor-array length. Does not assert indexed reads at duplicate ends |
| `precomputedFactCompositionMatchesDirectComputation` | 25 left/right combinations from two five-string sets including empty, ASCII, Latin-1 and surrogate halves; composed record equals direct `of(left + right)` |

| Test in [`M3ArrayNativeTest`][native-tests] | Assertions actually present |
| --- | --- |
| `javaFallbackHasExactCompareAndMaterializationSemantics` | Equality and negative comparison, three-array concatenation, oversized comparison range and null child refusal. The name does not force `loaded == false`; no reset or isolated classloader is used |
| `optionalNativeProviderMatchesJavaAcrossChunkAndUnicodeBoundaries` | Returns immediately if `m3.arrays.native.path` is absent/blank. Otherwise explicitly loads and compares seeded arbitrary UTF-16 at lengths 0, 1, 1023, 1024, 1025 and 4097, checking equality and exact changed-unit difference; concatenates a split-surrogate pair |

The [C test][native-checks] contains five assertions: equal arrays, negative and
positive differences, an equal surrogate-pair subrange, and null pointers with
zero count. It tests the C comparison helper, not JNI allocation, exceptions,
loading, concatenation or pointer lifetime. No execution result is supplied
by this source inspection.

### Open coverage and qualification work

- Satisfy the missing snapshot `segmentCount` obligation and test empty/nonempty
  snapshot counts; preserve exact recipe/source lineage rather than patching a
  sealed owner ad hoc
- Test indexed reads at duplicate ends for all four types, including trailing,
  leading, repeated and nested empty children. The existing byte empty-child
  test exercises buffer export, not the failing navigation trace
- Add complete copy null/bounds/zero-count matrices, partial-effect custom
  providers, descriptor cursor preservation/coverage disagreement and typed
  zero-/one-child join identity cases
- Exercise deep nesting, length/count overflow, allocation failure, full-range
  identity, nested/empty slices and retained-owner lifetime/resource cost
- Cover `chars`, `codePointBefore`, `offsetByCodePoints`, `contentEquals`,
  `compareTo`, negative/oversized search starts and all isolated/split-surrogate
  slice boundaries; existing seam tests do not cover every default method
- Distinguish honest factories from mutable/inconsistent custom views and
  manually forged facts; test constructor boundaries and arithmetic failures
- Force Java fallback independently of native test order, then test missing or
  wrong-symbol libraries, repeated/null loads, JNI pending exceptions, nonzero
  offsets across native chunk boundaries and allocation/refusal behavior
- Record exact-head compiler, Java/native parity, JNI/GC/platform, recipe and
  resource evidence before qualification; neither these tests' presence nor
  successful loading establishes correctness or performance

## 8. Inspected source identities and linked authorities

All following paths are in public `hsoliwal/M3jdk21` at the revision above.
Complete fetched bodies were verified against their Git blob identities; these
are inspection identities, not executed test receipts. The seven non-descriptor
production Java files are the whole declared package at this snapshot.

| Path below `m3/arrays/` | Git blob SHA |
| --- | --- |
| `src/main/java/com/m3/arrays/M3Arrays.java` | `bf3afd670d77775290c1453bfe1a2142d9d1e644` |
| `src/main/java/com/m3/arrays/M3ByteArrayView.java` | `f60dd956471dc6badad8fea2f5b8627cdefb454d` |
| `src/main/java/com/m3/arrays/M3Utf16ArrayView.java` | `ddd9cda67033fe7bdb77410de4d9aef366a52499` |
| `src/main/java/com/m3/arrays/M3IntArrayView.java` | `6f49bd7a781fbf52bfbe5edf9e8f4e7dbd9a4275` |
| `src/main/java/com/m3/arrays/M3LongArrayView.java` | `0a5e8246e0dc99ffd7601bd93efc15a00f2626ac` |
| `src/main/java/com/m3/arrays/M3Utf16Facts.java` | `5a45402aa9fb1c43b19536f09e0cdb0cf7a01040` |
| `src/main/java/com/m3/arrays/M3ArrayNative.java` | `440694e15ba340e0e98d669269beed32f09c31da` |
| `src/main/java/module-info.java` | `e33652a77a4710ea1cb470ba1ae4a8ae01ad61af` |
| `src/test/java/com/m3/arrays/M3ArraysTest.java` | `d472b94f50903f17807adda03fdbf01ef0bff986` |
| `src/test/java/com/m3/arrays/M3ArrayNativeTest.java` | `68ea4b99778ec6fca2ced9da45a3650c5cc32755` |
| `src/main/native/m3_arrays_jni.c` | `78bb0a19eb8afd0abfd02f30b42063723312cbbf` |
| `src/main/native/m3_arrays.c` | `8fce5f1273472b8a2faeb24f43350b006f0356b0` |
| `src/main/native/include/m3_arrays.h` | `e4f8b49082eb7111b39b48a1d551230187fac3df` |
| `src/main/native/tests/m3_arrays_checks.c` | `8754edd107b7209210d576d182b38b9a6a47bab8` |
| `src/main/native/CMakeLists.txt` | `70d064ec1e41b6089105a471e831dc20c1e0ebe5` |
| `pom.xml` | `f5b325614521fbf076cef73d89871c92a75a48ea` |
| `README.md` | `d0fcd8c99ff81798596c0a00006c577266bada39` |

Additional exact source checks: `java/util/Arrays.java` blob
`9d1034e6fef6c8ecf9fe4b5b4032fbd2622fc79b` for the deterministic duplicate-end
trace; `java/lang/CharSequence.java` blob
`87bc4a5525a850a7323a784a1927e6221d706ed6` and `java/lang/Object.java` blob
`b3bcb015cb75e9bc8f4e8a1fe81c2892c0cdcab8` for the inherited-method boundary.
Their full paths are under `src/java.base/share/classes/`.

Read [AGENTS][agents], [CONTRIBUTING][contributing], the [module README][readme],
[porting invariant][invariant] and [name mapping][mapping] before changing this
family. Existing [package accounting](packages/README.md) remains a separate
inventory. Full body review of this bounded package does not complete every
M3 module, caller, receiver, source-custody family or runtime gate. No private
Synexia source was needed for this reference.

[factory]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/main/java/com/m3/arrays/M3Arrays.java
[byte]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/main/java/com/m3/arrays/M3ByteArrayView.java
[chars]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/main/java/com/m3/arrays/M3Utf16ArrayView.java
[ints]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/main/java/com/m3/arrays/M3IntArrayView.java
[longs]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/main/java/com/m3/arrays/M3LongArrayView.java
[facts]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/main/java/com/m3/arrays/M3Utf16Facts.java
[native]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/main/java/com/m3/arrays/M3ArrayNative.java
[byte-count]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/main/java/com/m3/arrays/M3ByteArrayView.java#L17
[char-count]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/main/java/com/m3/arrays/M3Utf16ArrayView.java#L22
[byte-snapshot]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/main/java/com/m3/arrays/M3Arrays.java#L83-L99
[char-snapshot]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/main/java/com/m3/arrays/M3Arrays.java#L162-L182
[join-check]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/main/java/com/m3/arrays/M3Arrays.java#L65-L69
[int-join]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/main/java/com/m3/arrays/M3Arrays.java#L282-L294
[locate]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/main/java/com/m3/arrays/M3Arrays.java#L329-L331
[binary-search]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/src/java.base/share/classes/java/util/Arrays.java#L1702-L1718
[readme]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/README.md
[module]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/main/java/module-info.java
[invariant]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/docs/m3jdk21-porting-invariant.md
[mapping]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/docs/name-mapping.json
[agents]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/AGENTS.md
[contributing]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/CONTRIBUTING.md
[pom]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/pom.xml
[cmake]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/main/native/CMakeLists.txt
[jni]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/main/native/m3_arrays_jni.c
[native-c]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/main/native/m3_arrays.c
[native-checks]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/main/native/tests/m3_arrays_checks.c
[tests]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/test/java/com/m3/arrays/M3ArraysTest.java
[native-tests]: https://github.com/hsoliwal/M3jdk21/blob/1eac151fe56bccdc33b24a0155cc1ac5b9959005/m3/arrays/src/test/java/com/m3/arrays/M3ArrayNativeTest.java
