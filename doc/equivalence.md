<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Equality strategies in M3JDK21

The existing `jdk.m3.collections.guava.base.Equivalence<T>` in module
`jdk.m3.collections` pairs an equivalence relation with a compatible hash
function. Its new `fromFunctions` factory accepts standard Java functional
interfaces, so applications can create a strategy without declaring a subclass.
The module already exports this package; existing provider names and methods
retain their contracts.

## Construct and reuse one strategy

```java
import java.util.HashMap;
import jdk.m3.collections.guava.base.Equivalence;

Equivalence<String> sameLength = Equivalence.fromFunctions(
        (left, right) -> left.length() == right.length(), String::length);

var labels = new HashMap<Equivalence.Wrapper<String>, String>();
labels.put(sameLength.wrap("cat"), "three letters");
assert labels.get(sameLength.wrap("dog")).equals("three letters");
assert !sameLength.equivalent("cat", "horse");
```

The predicate must be symmetric, transitive and stable for unchanged values.
The hash function must return the same integer for equivalent values. Different
values may share a hash. The factory cannot establish these laws for arbitrary
callbacks; verify the selected domain, including collisions and boundary values.
No reflective field inspection, normalization or result caching is performed.

Each factory call creates a distinct strategy. Store and reuse that strategy
for wrappers intended to compare equal. Two separately created strategies are
not equal merely because their callbacks happen to produce the same results.
Wrappers retain the referenced value; they do not snapshot mutable values.
Keep equivalence-relevant values and captured callback state stable while a
wrapper is used as a hash-map or hash-set key.

## Null handling and callback invocation

| Operation | Result or invocation |
| --- | --- |
| `equivalent(value, value)` | `true`, without invoking either callback |
| `equivalent(null, null)` | `true`, without invoking either callback |
| Exactly one comparison argument is null | `false`, without invoking either callback |
| Two distinct, non-null comparison arguments | One predicate call, in argument order |
| `hash(null)` | `0`, without invoking either callback |
| `hash(nonNullValue)` | One primitive `ToIntFunction.applyAsInt` call |

Null callbacks are rejected when the factory is called. The equality predicate
is validated before the hash function. Callback exceptions propagate unchanged.
Comparison never invokes the hash callback, and hashing never invokes the
comparison callback. The strategy adds no synchronization; its thread safety
depends on the supplied callbacks and their captured state.

The new callback strategy is not serializable. Existing serializable built-in
strategies and their wrappers retain their existing serialization behavior.
Serializing a wrapper around a nonserializable callback strategy is unsupported.

## Existing composition remains available

`equals()` supplies ordinary object equality and hashing; `identity()` supplies
reference identity and identity hashing. `onResultOf` compares projected keys,
`pairwise` compares ordered iterable elements, `equivalentTo` creates a matching
predicate, and `wrap` supplies a key with the strategy's equality and hash rules.
All operate with a strategy returned by `fromFunctions`.

Array content comparisons can reuse the existing JDK operations explicitly:

```java
import java.util.Arrays;

Equivalence<byte[]> bytes = Equivalence.fromFunctions(
        Arrays::equals, Arrays::hashCode);
```

This does not change array identity, `Object.equals`, `String.equals`, or the
behavior of ordinary collection keys. Callers select the strategy explicitly.
The array example retains caller-owned arrays and requires stable contents
while wrappers are used as keys. There is no automatic deep object-graph policy.

## Use the installed provider adapters

The provider bridge uses
`jdk.m3.collections.eclipse.api.block.HashingStrategy<T>`, the equality strategy
shared by the module's collection providers. A callback policy can supply this
strategy directly:

```java
import jdk.m3.collections.eclipse.api.block.HashingStrategy;

HashingStrategy<String> hashing = sameLength.asHashingStrategy();
assert hashing.equals("cat", "dog");
assert hashing.computeHashCode("cat") == sameLength.hash("cat");
assert Equivalence.fromHashingStrategy(hashing) == sameLength;
```

The bridge delegates to `equivalent` and `hash`, preserving the callback policy's
reference and null shortcuts, callback invocation counts and exception behavior.
Converting it back with `fromHashingStrategy` returns the original callback policy
by identity. Two bridges of the same policy compare equal and have equal hash
codes; bridges of separately created callback policies remain unequal.

`fromHashingStrategy` also accepts an existing `HashingStrategy<? super T>`.
The resulting equivalence applies its own reference and null rules. Converting
that equivalence back returns the original strategy, including that strategy's
original null behavior.

`HashingStrategy` extends `Serializable`, so the bridge has that marker even
when its callback policy cannot be serialized. Serializing a bridge around a
policy from `fromFunctions` fails with `NotSerializableException`; converting
the bridge back does not make the original callback policy serializable.

## Compatibility and verification

The addition has a distinct method name and imposes no new abstract obligations
on existing subclasses. Existing `equals`, `identity`, `onResultOf`, `pairwise`,
`wrap`, `equivalentTo`, and `BiPredicate` bridge descriptors remain unchanged.

The target regression is
`test/jdk/jdk/m3/collections/EquivalenceFromFunctionsTest.java`. With a built
M3JDK21 image and configured jtreg, run:

```sh
make test TEST="test/jdk/jdk/m3/collections/EquivalenceFromFunctionsTest.java"
```

EqualsVerifier is used in the separate Synexia qualification suite to check real
wrapper equality contracts. It is a test dependency and is not required by
M3JDK applications or by this module's runtime implementation. Existing Apache
Commons builder patterns informed the comparison/hash review; this change does
not add a duplicate builder family or substitute reflective equality globally.
