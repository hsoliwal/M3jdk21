<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Additional functional interfaces

M3JDK 21 adds general-purpose functional interfaces inside `java.base`:

- `java.util.function.ext` supplies shapes absent from the original JDK package.
- `java.util.function.checked` supplies independent interfaces with an explicit
  `E extends Throwable` parameter.

Use the existing `java.util.function` interfaces whenever they fit. Their 43
public types, methods and defaults are unchanged. `Runnable` and `Comparator`
also retain their original owners and behavior. Applications using the new
interfaces require an M3JDK image containing these additions.

## Coverage and naming

For zero, one or two arguments, the interface catalogue covers every positional
combination of references and Java's eight primitive types. Results can be a
reference, any primitive, or `void`. This is 910 distinct shapes:
`10 + 9 * 10 + 9 * 9 * 10`. Standard interfaces already cover 42 of them;
the extension package fills the other 868. The checked package provides all
910 shapes with a declared throwable type.

Operators, comparators and higher arities add useful roles beyond that matrix.
There are 920 new unchecked interfaces and 965 checked interfaces, 1,885 total.
These counts include separately named Boolean operators, primitive comparator
contracts, and reference-argument functions, consumers and predicates of arity
three through sixteen. They do not mean that every domain-specific callback
contract or unbounded arity has a dedicated type.

| Shape | Interface example | Functional method |
| --- | --- | --- |
| No input, primitive result | `ByteSupplier` | `getAsByte()` |
| Primitive input, no result | `FloatConsumer` | `accept(float)` |
| Primitive input, Boolean result | `CharPredicate` | `test(char)` |
| Primitive input, reference result | `ByteFunction<R>` | `apply(byte)` |
| Reference input, primitive result | `ToFloatFunction<T>` | `applyAsFloat(T)` |
| Primitive conversion | `ByteToDoubleFunction` | `applyAsDouble(byte)` |
| Same-domain operation | `ByteUnaryOperator` | `applyAsByte(byte)` |
| Two equal primitive inputs | `IntBiPredicate` | `test(int, int)` |
| Two different primitive inputs | `IntLongFunction<R>` | `apply(int, long)` |
| Two inputs, primitive result | `IntLongToDoubleFunction` | `applyAsDouble(int, long)` |
| Object then primitive | `ObjIntToCharFunction<T>` | `applyAsChar(T, int)` |
| Primitive then object | `LongObjConsumer<T>` | `accept(long, T)` |
| Primitive ordering | `FloatComparator` | `compare(float, float)` |
| Three reference inputs | `TriFunction<T,U,V,R>` | `apply(T, U, V)` |
| Four through sixteen inputs | `Function4` through `Function16` | `apply(...)` |

The checked counterparts have the same simple names in their own package, with
`E` as the final type parameter. For example,
`java.util.function.checked.ObjIntToCharFunction<T,E>` declares
`char applyAsChar(T value, int index) throws E`.

Predicates are the canonical Boolean-result shapes; there is no duplicate
`ToBooleanFunction<T>`. Boolean unary and binary operators are explicit
same-domain roles with `applyAsBoolean`, distinct from predicates' `test`.
A comparator has ordering obligations that an arbitrary int-returning function
does not have. Primitive positions in the functional method remain primitives.
That signature property does not promise that a captured lambda never allocates.

## Examples

An indexed character operation uses the existing positional naming scheme:

```java
import java.util.function.ext.ObjIntToCharFunction;

ObjIntToCharFunction<CharSequence> characterAt = CharSequence::charAt;
char character = characterAt.applyAsChar("M3JDK", 2);
```

An index is an ordinary `int` or `long` position. For an object-first callback
returning no result, the existing `ObjIntConsumer<T>` already fits. New
`Indexed*` aliases are unnecessary.

Typed checked exceptions pass through without wrapping:

```java
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.checked.Function;

Function<Path, String, IOException> read = Files::readString;
Function<Path, Integer, IOException> length = read.andThen(String::length);
int size = length.apply(Path.of("input.txt")); // caller handles IOException
```

The exception type can be a checked exception, a runtime exception, an error,
or another direct subclass of `Throwable`. Combining operations with different
checked exception families requires choosing a common declared supertype.
Checked interfaces do not inherit nonthrowing JDK SAMs and do not sneakily throw,
wrap, catch, suppress or substitute results for exceptions.

## Default-operation contracts

Consumers' `andThen` invokes the first operation before the second. The second
operation is skipped if the first throws. Functions returning references pass
the exact result, including null, to their `andThen` operation. Predicate `and`
and `or` short circuit. Primitive unary operators supply `compose`, `andThen`
and `identity`.

Composition checks a supplied collaborator for null immediately, before
returning a composed operation. It invokes no callback during construction.
Exceptions propagate as the same throwable object. Checked operations neither
set nor clear thread interruption status.

Primitive comparators follow `Comparator`'s ordering contract. `reversed`
swaps the operands instead of negating a result, so `Integer.MIN_VALUE` is safe.
`thenComparing` invokes the secondary comparator only for a zero primary result.
Unchecked primitive comparators provide `naturalOrder`; floating-point order
uses `Float.compare` or `Double.compare`, including NaN and signed zero.

These interfaces impose no serialization, resource ownership, purity,
memoization, scheduling, asynchronous execution, cancellation, or thread-safety
guarantee. Those are separate contracts of the caller and implementation.

## Compatibility boundary

New subpackages preserve the original `java.util.function` wildcard namespace.
Adding a top-level `TriFunction` to that original package could make previously
valid source ambiguous when it also imports a library's `TriFunction` through a
wildcard. Opting into new packages can introduce new simple-name overlaps, so
use explicit imports when mixing checked and unchecked types.

This addition introduces no overloads on existing streams, collections,
executors or other consumers. A shape-compatible donor interface remains a
different nominal Java type; use a method reference where appropriate. For
example, Eclipse Collections' `IntFunction<T>` maps a reference to `int`, while
the JDK's `IntFunction<R>` maps `int` to a reference. Names alone do not establish
equivalence.

The product tests independently cover complete shape enumeration, typed lambda
assignments, primitive descriptors, callback behavior, checked failures,
generic-SAM refusal and the wildcard-import counterexample. Their explicit
component modes do not stand in for the default checks of actual image exports
and the image's own compiler. Build and qualify the complete M3JDK image before
release.

## References

- [JDK 21 functional-interface naming and scope](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html)
- [JLS 21 simple type names](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.5.5.1)
- [JLS 21 functional interfaces](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.8)
- [Eclipse Collections primitive API inventory](https://eclipse.dev/collections/javadoc/11.1.0/org/eclipse/collections/api/block/function/primitive/package-summary.html)
- [Apache Commons checked-function precedent](https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/function/FailableFunction.html)

The implementations are original contributions. Donors inform the coverage
catalogue; their implementation bodies and runtime libraries are not included.
