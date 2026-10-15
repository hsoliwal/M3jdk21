<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Internal class and member views

[Guide](README.md) · [Runtime owners](runtime-owners.md) · [Class index](class-index-reference.md) · [Metadata helpers](internal-metadata-lanes-reference.md) · [Mapped storage and weak boundaries](mapped-string-storage-reference.md)

This reference reviews the complete bodies of nine `jdk.internal.mindex` owners in
`java.base` at public source revision `e7d21edf0f998c3c53ab0d0909972a4a97dfc471`.
They expose frozen class/member metadata and explicit live-JVM conversion boundaries.
They are internal counterparts, not a replacement for every `java.lang.Class` or
`java.lang.reflect` operation. The package is not exported by the reviewed module descriptor.
All repository builds, tests, recipes, native execution and performance checks are **NOT_RUN**
for this documentation pass.

## Scope and source authority

| Complete primary body | Responsibility | Git blob |
| --- | --- | --- |
| [M3Class](../../src/java.base/share/classes/jdk/internal/mindex/M3Class.java) | Class-row view, hierarchy, declared-member projections and live Class forwarding | `b95eee3c820f6054b4f70507dee101c9f456302f` |
| [M3Member](../../src/java.base/share/classes/jdk/internal/mindex/M3Member.java) | Kind-neutral member-row metadata and reflective-element conversion | `d5f8e07afc304379d5165c9c5c1659855938b5b0` |
| [M3Field](../../src/java.base/share/classes/jdk/internal/mindex/M3Field.java) | Field-only view and Field conversion | `fad031da70ce813f52e5fc06479542a224bcd81a` |
| [M3Method](../../src/java.base/share/classes/jdk/internal/mindex/M3Method.java) | Method-only view and Method conversion | `6bc2fe9176bdd9c95d732ecad105f03b62f4385e` |
| [M3Constructor](../../src/java.base/share/classes/jdk/internal/mindex/M3Constructor.java) | Constructor-only view and Constructor conversion | `4d708115e139a4ab938a4f9abd856f7fc98c5c00` |
| [M3RecordComponent](../../src/java.base/share/classes/jdk/internal/mindex/M3RecordComponent.java) | Record-component-only view and RecordComponent conversion | `55f82a8486055d50e24d1a9b12b0636f9db876ed` |
| [M3ClassFlags](../../src/java.base/share/classes/jdk/internal/mindex/M3ClassFlags.java) | Class-kind bits derived at admission | `fc2152f63f4c404bce630b043d9138eaf96cb93e` |
| [M3MemberFlags](../../src/java.base/share/classes/jdk/internal/mindex/M3MemberFlags.java) | Member-kind attributes beside the modifier lane | `f68a952217d3eead4ed10a43de49c240d6b790f1` |
| [M3MemberKind](../../src/java.base/share/classes/jdk/internal/mindex/M3MemberKind.java) | Four explicit stored kind codes | `d211e6bc1e72ccc8e0ff821d26b3e30361750bc9` |

Dependencies are deliberately a different scope. Selected `M3CI` factory, lookup,
discovery/freeze, resolver, order and hash sections explain these views; selected
`M3Boundary` and `M3CB` sections explain weak mirrors and caller invalidation.
`M3Descriptor` is read for name/descriptor encoding. `M3TQ` range-identity sections
establish only a separate candidate-facts boundary. Selected `Class` native/reflective
methods and `VM` startup-state sections are dependency reads, not complete JDK-class or
VM reviews. The linked companion references own those broader internal contracts.

Use the [porting invariant](../../m3/docs/m3jdk21-porting-invariant.md),
[naming map](../../m3/docs/name-mapping.json), [borrowing policy](../../SYNEXIA_APACHE_BORROWING_POLICY.md)
and [Class Bridge packet](../../m3/tooling/cb/README.md) for placement and lineage.
Reusable convergence/recipes retain their existing authority; target runtime acceptance
belongs to M3JDK21. Existing per-file licenses and notices remain unchanged.
This guide does not modify sealed source or advance the ordered receiving phases.

## Identity, lifetime and materialization

Each of the six view classes has exactly two retained fields: a final `M3CI owner` and
a final integer `row`. Package-private constructors require a non-null owner and check
the row against `owner.size()` for classes or `owner.memberSize()` for members.
The four typed-member constructors additionally reject a row of the wrong kind with
`IllegalArgumentException`. A negative or past-end row fails `Objects.checkIndex`.
Callers obtain views through the index or class methods rather than public constructors.

- `owner()` returns the same index object. `row()` is an index-local coordinate, not
  a byte offset, native pointer, globally assigned class ID or portable image identity
- A member's row is in the whole index's member table. `M3Class.memberAt(ordinal)` takes
  a per-class ordinal and converts it to that global member row; the two numbers are
  not interchangeable
- `loaderSpaceId()` identifies a defining-loader namespace within this snapshot.
  Bootstrap is zero. Other IDs follow discovery of loader identities, so different
  root sets/order or VM sessions need not produce the same IDs or rows
- Name strings are references to existing immutable JDK Strings in `M3CI`'s text
  table. These views do not introduce a second canonical spelling store or global
  text interner. `getName()`-style adapters call String `toString()`; they do not
  promise a new spelling allocation
- View factories and enumeration methods construct wrappers on demand, and array
  results are fresh arrays. The snapshot was already prepared using reflection;
  this is lazy wrapper/reflection-object projection, not lazy class-metadata admission
- A retained view strongly retains its index and metadata. A standard index retains
  only weak Class/ClassLoader mirrors at its JVM boundary. Compact snapshots retain
  counts without weak-reference objects. Holding a view therefore does not by itself
  keep the indexed Class or defining loader live
- Successfully returned `Class`, `Field`, `Method`, `Constructor` or `RecordComponent`
  objects are ordinary caller-held reflection objects. Retaining those results can
  retain the corresponding live runtime objects; that is distinct from the weak view
- Views have no `close`, invalidation, reload or epoch API. Frozen metadata remains
  readable when a weak mirror disappears. A caller using `M3CB` must follow its
  invalidation contract before redefinition/retransformation or relevant access changes;
  invalidating that session does not mutate already-held view metadata

`M3Class.equals` accepts only another `M3Class` with identical owner reference and row;
`M3Member.equals` does the equivalent for `M3Member`. Their `hashCode()` is
`31 * System.identityHashCode(owner) + row`. A separately compiled snapshot is a different
owner even if it describes the same live classes and has an equal fingerprint.

**The four typed wrappers do not override `equals` or `hashCode`.** `M3Field`, `M3Method`,
`M3Constructor` and `M3RecordComponent` use Object identity. Two separate enumerations
can return unequal wrapper objects for the same owner/row. Compare the owner reference,
row and required kind explicitly when that is the intended identity; do not infer
reflection-object equality from the wrapper class's name. A generic `M3Member` is not
equal to a typed wrapper for the same row.

Every view's `structuralHash64()` returns the stored class/member hash from `M3CI`.
Those hashes incorporate snapshot-local coordinates and metadata; they neither define
view equality nor authenticate class bytes. They are not collision-free identities.
The index fingerprint includes text and structural lanes but is still a candidate key,
not a promise of portable numbering, definition authentication or current runtime validity.
Likewise, `M3TQ` text facts require the exact immutable source/range association; they
are not class/member identity or a substitute for exact metadata lookup.

## M3Class metadata and hierarchy

These operations read the frozen index and do not require a live Class mirror:

| Declared entry points | Result and boundary |
| --- | --- |
| `name()`, `getName()` | Admission-time `Class.getName()` spelling, including its special array/hidden forms; not always a Java source name |
| `simpleName()`, `getSimpleName()` | Admission-time simple name; an empty string is a possible value, not absence |
| `canonicalName()`, `getCanonicalName()` | Optional spelling, with the getter returning `null` when absent |
| `packageName()`, `getPackageName()` | Stored package-name spelling, including an empty string where the JDK supplies one |
| `moduleName()` | Optional module-name spelling; an unnamed module has no name. This does not return a Module object |
| `typeName()`, `getTypeName()` | Stored `Class.getTypeName()` spelling |
| `modifiers()`, `getModifiers()` | Stored JDK modifier integer |
| `flags()`, `hasFlag(int)` | Independent M3 class-kind mask; `hasFlag` means any requested bit overlaps, not all bits are present |
| `superclass()`, `getSuperclass()` | Optional same-owner view; getter returns `null` for no stored relationship |
| `componentType()`, `getComponentType()` | Optional same-owner array-component view; getter returns `null` when absent |
| `nestHost()`, `getNestHost()` | Optional same-owner host; getter falls back to `this` if the stored host is absent |
| `declaringClass()`, `getDeclaringClass()` | Optional same-owner declaring class; getter returns `null` when absent |
| `enclosingClass()`, `getEnclosingClass()` | Optional same-owner enclosing class; getter returns `null` when absent |
| `interfaceCount()`, `interfaceAt(int)`, `getInterfaces()` | Direct interfaces in the observed Class declaration order; bulk getter creates a new view array |
| `directSubtypeCount()`, `directSubtypeAt(int)` | Reverse direct superclass/interface edges represented within this snapshot, in row order; not all loaded classes in the VM |
| `supertypeCount()`, `supertypeAt(int)` | Stored transitive hierarchy rows in ascending row order, including the row itself; interfaces include the Object relation added by the index |
| `isAssignableFrom(M3Class)` | Requires non-null candidate and the same owner object; different snapshots throw `IllegalArgumentException` |
| `annotationCount()`, `annotationTypeNameAt(int)` | Distinct declared-annotation type names frozen at admission, sorted by local symbol ID; no annotation values or inherited-annotation expansion |
| `permittedSubclassCount()`, `permittedSubclassNameAt(int)` | Names in the observed `Class.getPermittedSubclasses()` order, or zero entries when absent |

The twelve kind predicates are `isInterface`, `isEnum`, `isAnnotation`, `isRecord`,
`isArray`, `isPrimitive`, `isSealed`, `isHidden`, `isSynthetic`, `isAnonymousClass`,
`isLocalClass` and `isMemberClass`; each tests the correspondingly named stored bit.
They do not re-query a live class on each call.

All ordinal operations check `0 <= ordinal < count` through the index before reading
the corresponding lane. Absent relationships are represented by a negative index sentinel
and converted into the documented Optional/null/self result; absence is not row zero.

Assignability uses the indexed transitive-supertype relation, with recursive array-component
handling for reference-array covariance and exact component-row equality when either
component is primitive. Its candidate is a type view, not an object instance. The stored
supertype enumeration is not expanded into every covariant array type that may be assignable.
Use `isInstance`/`cast` only through the separate live-Class boundary below.

## Declared members and order

| `M3Class` entry points | Contract |
| --- | --- |
| `memberCount()`, `memberAt(int)` | All declared member kinds in this class's frozen row range; ordinal is checked, result is a fresh generic `M3Member` |
| `memberRows(CharSequence)` | Fresh whole-table member-row array for the exact name in this declaring class, potentially several kinds/overloads; null is rejected and an unknown name yields an empty array |
| `getDeclaredFields()`, `getDeclaredMethods()`, `getDeclaredConstructors()` | New arrays of typed wrappers selected by kind from the class's stored rows; inherited members are excluded |
| `getRecordComponents()` | `null` for a non-record; otherwise a new array selected from RECORD_COMPONENT rows, including an empty array for a record with none |
| `getDeclaredClasses()` | New same-owner views obtained by scanning stored declaring-class relationships in ascending class-row order |
| `getNestMembers()` | New same-owner views whose stored nest host matches; ascending class-row order, or an empty array if no host is stored |
| `getPermittedSubclasses()` | `null` when not sealed; otherwise maps permitted names back into the same loader-space table, preserves surviving name order and omits unresolved rows |
| `getDeclaredField(String)` | Frozen exact-name search filtered to FIELD; null is rejected, and a missing field throws `NoSuchFieldException` with class/name context |

The index discovers a bounded closure of supplied roots and relevant hierarchy/nest/declaring/
enclosing/permitted classes. It does not enumerate every class in every loader. Consequently,
reverse subtypes, declared classes and nests are views over that closure, not a VM-wide registry.
The permitted-name count and resolved permitted-view count need not be assumed identical:
the resolver explicitly omits a name that does not resolve in the requested loader space.

Class rows are sorted by snapshot-local loader-space ID, then `Class.getName()`. Member
rows are sorted by declaring class row, **name symbol ID**, kind code, then descriptor
symbol ID. Symbols receive IDs on first admission; those IDs are not lexical ranks.
Filtering a member kind preserves this stored order. Do not promise source-declaration,
alphabetical, reflection-array or portable cross-snapshot order for declared-member arrays.
In particular, **record-component order is not explicitly preserved by the frozen member
sort**. Code requiring Class's record declaration order must use the live boundary or
separately establish the required order; the existing record test only checks the count.

Direct interfaces are the explicit order-preserving exception. Permitted names preserve
their observed JVM order. Direct subtype/transitive-supertype rows and declared/nest class
scans use row order. Annotation names are deduplicated and sorted by symbol ID; declared
exception descriptors preserve the reflection exception-type array order.

## Live reflection boundaries

`tryClass()` asks the weak boundary for this row and returns an Optional. `toClass()`
throws `IllegalStateException` if the mirror is unavailable. Compact snapshots cannot
satisfy that conversion, including for bootstrap classes. A successful earlier
`tryClass()` does not guarantee a later lookup succeeds unless something keeps the
returned Class live. These methods do not load a class by name to recreate a lost mirror.

The following M3Class methods all require `toClass()` and delegate to the returned Class:

| Operations | Why the live boundary matters |
| --- | --- |
| `isInstance(Object)`, `cast(Object)` | JVM object/type semantics. With a live class, null and wrong-type behavior follows Class; with no mirror the boundary fails first |
| `getClassLoader()`, `getModule()`, `getPackage()`, `getProtectionDomain()` | Live runtime objects and runtime access checks, not reconstructed metadata objects; the bootstrap class loader is `null` |
| `desiredAssertionStatus()`, `getResource(String)`, `getResourceAsStream(String)`, `getSigners()` | Assertion/resource/security behavior belongs to Class and its loader/module; resources may be absent and callers own returned streams |
| `getGenericSuperclass()`, `getGenericInterfaces()`, `getTypeParameters()` | Generic Type/TypeVariable structures are not encoded by the view's name/descriptor lanes |
| `isAnnotationPresent(Class)`, `getAnnotation(Class)`, `getAnnotations()`, `getDeclaredAnnotations()`, `getAnnotationsByType(Class)`, `getDeclaredAnnotation(Class)`, `getDeclaredAnnotationsByType(Class)` | Actual annotation objects, values, inheritance and repeatable-annotation behavior are live reflection, distinct from frozen annotation type names |

This is forwarding to existing JDK methods, not a guarantee that every caller-sensitive,
security-manager, agent or module-access case is identical to a direct caller invoking
Class. The views do not grant access privileges, suppress access checks, make members
accessible or install a new reflection implementation.

Lookups split into two mechanisms:

1. `getDeclaredField` and the declared-array enumerations above use only frozen metadata
2. `getDeclaredMethod(String, Class<?>...)`, `getDeclaredConstructor(Class<?>...)`,
   `getField(String)`, `getMethod(String, Class<?>...)`, `getConstructor(Class<?>...)`,
   `getFields()`, `getMethods()` and `getConstructors()` first invoke the live Class
   lookup/enumeration, then map each result back to this snapshot

The second mechanism preserves the live lookup's selection, including method return-type
selection, and its array order for `getFields`/`getMethods`/`getConstructors`.
Its wrapper requires non-null name and parameter-array arguments where declared; it does
not add support for a null varargs array. Reflection lookup failures can propagate
`NoSuchFieldException`/`NoSuchMethodException`, security or linkage failures.
Missing mirrors fail via `toClass()` before a reflection lookup can be completed.

The private `fieldView`, `methodView` and `constructorView` adapters first resolve the
reflection result's actual declaring Class through `owner.fromClass`, which verifies the
live defining-loader and Class identity. They then locate the exact kind/name/descriptor
row. An outside declaring class or missing metadata causes `IllegalStateException`;
they do not silently expand or rebuild the snapshot. This is important for inherited
public members: the wrapper belongs to the declaring class's row, not automatically the
class on which `getField`/`getMethod` was called.

`M3Class.toString()` returns the stored name spelling, without Class's `class`/`interface`
prefix. It is a display projection, not a signature or serialized identity.

## Member views

### Shared name, descriptor and optional-metadata contracts

Member kinds and their flags are frozen once. Members expose an exact stored
name and descriptor, not a reparsed source signature. Field/record-component descriptors
come from their type's `Class.descriptorString()`. Methods encode parameter descriptors in
declaration order and their return descriptor; constructors use a void return descriptor.
Constructor names are `Constructor.getName()` spellings (the declaring class name), not
the class-file token `<init>`. The descriptor includes a method's return type, so it can
distinguish rows that a name/parameter-only lookup must select using reflection rules.

`M3Descriptor` deliberately uses VM-owned `descriptorString()` for hidden types rather
than deriving it by replacing dots in a name. A hidden spelling is not necessarily a
nominal descriptor accepted by `ClassDesc.ofDescriptor`, a resolvable name or portable
between VM sessions. Equal nominal descriptor strings also do not identify a defining
loader. See the [helper reference](internal-metadata-lanes-reference.md).

Annotation metadata retains declared annotation **type names**, not values, parameter/type
annotations, generic signatures, default values or repeatable-container expansion.
An annotation count of zero means no admitted declared runtime annotation types, not
absence of every possible source/class-file annotation. Exception metadata stores declared
exception type descriptors for methods/constructors; fields and record components have
empty exception lanes. Record-component rows store modifier zero and the
RECORD_COMPONENT flag. Generic throws/type-variable detail is not encoded there.

### M3Member

| Declared entry points | Contract |
| --- | --- |
| `owner()`, `row()`, `kind()` | Index identity, whole-table member coordinate and decoded `M3MemberKind` |
| `declaringClass()` | Fresh M3Class view of the member's stored declaring row |
| `name()`, `descriptor()`, `modifiers()`, `flags()`, `hasFlag(int)` | Frozen text/JDK modifiers/M3 mask; `hasFlag` tests any overlap |
| `annotationCount()`, `annotationTypeNameAt(int)` | Declared annotation-name lane; ordinal checked against that member's count |
| `exceptionCount()`, `exceptionTypeDescriptorAt(int)` | Declared exception-descriptor lane; ordinal checked against that member's count |
| `structuralHash64()` | Stored candidate hash, separate from `hashCode()` and exact identity |
| `tryReflectiveElement()`, `toReflectiveElement()` | Optional live AnnotatedElement or throwing conversion |
| `toString()` | Declaring name + `#` + member name + descriptor |
| `equals(Object)`, `hashCode()` | Same M3Member owner-reference/row identity described above |

### Typed member views

The common typed surface is `owner()`, `row()`, `name()`, `getName()`, `descriptor()`,
`annotationCount()`, `annotationTypeNameAt(int)`, `structuralHash64()` and `toString()`.
These use the same frozen lanes and checked annotation ordinals. Their remaining methods are:

| Owner | Declaring/modifier/flag/exception methods | Live conversion |
| --- | --- | --- |
| `M3Field` | `getDeclaringClass()`, `getModifiers()`, `isSynthetic()`, `isEnumConstant()` | `tryField()`, `toField()` |
| `M3Method` | `getDeclaringClass()`, `getModifiers()`, `isSynthetic()`, `isBridge()`, `isVarArgs()`, `isDefault()`, `exceptionCount()`, `exceptionTypeDescriptorAt(int)` | `tryMethod()`, `toMethod()` |
| `M3Constructor` | `getDeclaringClass()`, `getModifiers()`, `isSynthetic()`, `isVarArgs()`, `exceptionCount()`, `exceptionTypeDescriptorAt(int)` | `tryConstructor()`, `toConstructor()` |
| `M3RecordComponent` | `getDeclaringRecord()`; no typed modifier/exception API | `tryRecordComponent()`, `toRecordComponent()` |

`M3Field.toString()` and `M3RecordComponent.toString()` render declaring name + `#` + name.
`M3Method.toString()` adds its descriptor; `M3Constructor.toString()` renders constructor
name + descriptor. These intentionally small display strings are not reflection's complete
formatted declaration strings. None of these wrappers implements `java.lang.reflect.Member`
or `AnnotatedElement`; matching method names do not imply those interfaces are supported.

All `try…` member conversions call `M3CI.resolveReflectiveElement(row)`. The resolver
gets the weak declaring Class, enumerates that kind's declared reflection elements and
matches exact name plus descriptor. No reflection-object cache is retained in the view.
The typed methods additionally filter/cast to the expected reflection type.
Missing mirrors, missing matches, or a resolver-caught `LinkageError`/`SecurityException`
yield an empty Optional. That does not mean every possible exception is swallowed.
The `to…` counterparts turn empty into `IllegalStateException`; the exception wording
does not distinguish unloading from compact mode or every other unavailability cause.

There is no member `get`, `set`, `invoke`, `newInstance`, `setAccessible`, generic-type,
parameter-name or annotation-object API in these typed wrappers. Convert to the appropriate
reflection object and obey its normal checks for those operations. There is no automatic
fallback from an unavailable view conversion to a newly loaded same-named class.

## Flags and kind codes

The flag utility classes have private constructors. All `of(...)` overloads reject null
and inspect the supplied live reflection object; flag values are then retained beside
the JDK modifier integer rather than replacing its encoding.

| M3ClassFlags constant | Value | Predicate used by `of(Class<?>)` |
| --- | --- | --- |
| `INTERFACE` | `1 << 0` | `isInterface()` |
| `ENUM` | `1 << 1` | `isEnum()` |
| `ANNOTATION` | `1 << 2` | `isAnnotation()` |
| `RECORD` | `1 << 3` | `isRecord()` |
| `ARRAY` | `1 << 4` | `isArray()` |
| `PRIMITIVE` | `1 << 5` | `isPrimitive()` |
| `SEALED` | `1 << 6` | `isSealed()` |
| `HIDDEN` | `1 << 7` | `isHidden()` |
| `SYNTHETIC` | `1 << 8` | `isSynthetic()` |
| `ANONYMOUS` | `1 << 9` | `isAnonymousClass()` |
| `LOCAL` | `1 << 10` | `isLocalClass()` |
| `MEMBER` | `1 << 11` | `isMemberClass()` |

Bits can coexist, such as ANNOTATION and INTERFACE. `hasFlag(0)` is false; combining
bits in an argument asks whether any are present. Unknown mask bits are not validated.

`M3MemberFlags` defines `SYNTHETIC = 1`, `BRIDGE = 1 << 1`, `VARARGS = 1 << 2`,
`DEFAULT_METHOD = 1 << 3`, `ENUM_CONSTANT = 1 << 4`, `RECORD_COMPONENT = 1 << 5`.
Its four `of` overloads are intentionally different:

- `of(Field)`: synthetic and enum-constant bits
- `of(Method)`: synthetic, bridge, varargs and default-method bits
- `of(Constructor<?>)`: synthetic and varargs bits
- `of(RecordComponent)`: RECORD_COMPONENT only after the null check

`M3MemberKind` defines FIELD(0), METHOD(1), CONSTRUCTOR(2), RECORD_COMPONENT(3).
Its private enum constructor stores the explicit code; `code()` returns it and
`fromCode(int)` accepts exactly 0–3, rejecting every other value with
`IllegalArgumentException`. Use those explicit codes rather than inferring a persistence
format from `ordinal()`. Compiler-generated `values()`/`valueOf(String)` and inherited
Enum behavior are ordinary Java enum APIs, not separately authored index operations.

## Tests and runtime prerequisites

The following test bodies were read, not executed. Their assertions support bounded
contracts; they are not a complete parity or platform matrix.

| Source read | Relevant assertions and limits |
| --- | --- |
| [CBProbe](../../test/jdk/jdk/internal/mindex/cb/com/m3/cb/CBProbe.java) and [VmFixture](../../test/jdk/jdk/internal/mindex/cb/com/m3/cb/VmFixture.java) | Class-name/modifier/kind comparisons, direct-interface order, field/method/constructor counts, declared-method set and lookup round trips, inherited field declaring owner, two record components, first permitted-class identity, live cast/instance behavior, pairwise hierarchy checks, distinct defining loaders/hidden classes, no fixture initialization, compact bridge fallback, cross-snapshot refusal, budget/cancellation, repeat fingerprint and loader unloading. Record-component order, every typed predicate and every unavailable conversion are not exhaustively asserted |
| [DescriptorTest](../../test/jdk/jdk/internal/mindex/descriptor/com/m3/descriptor/DescriptorTest.java) | Independent Class/MethodType descriptor comparisons, nominal/hidden/lambda/local/anonymous types, array ranks, same-name different-loader descriptors, nulls and no fixture initialization; tests the descriptor dependency, not wrapper enumeration/equality |
| [CBTest](../../m3/tooling/cb/src/test/java/com/m3/cb/CBTest.java) | Recipe generation/fixed point, template/source hashes, tamper/parser/drift refusal, strict Java-21 patch-module compilation, generated probe runs and java.base-only jdeps expectation. These are source-defined tooling checks, not evidence they ran on this documentation candidate |
| [CloseTest](../../m3/tooling/cb/src/test/java/com/m3/cb/CloseTest.java) and [NativeTest](../../m3/tooling/cb/src/test/java/com/m3/cb/NativeTest.java) | Historical backing-close rewrite and JNI-text recipe checks. The close target still names `MIndexStringBacking`; these bodies do not test the nine view contracts or prove current native execution |

The current CBProbe header requests interpreter and mixed runs with class unloading;
DescriptorTest requests default, interpreter and `-XX:-CompactStrings` runs. Directly
reproducing these tests requires a matched fork image, a configured jtreg harness and
the correct OpenJDK build configuration. Consult the
[first-build checklist](../../CONTRIBUTING.md#first-build-prerequisites-outputs-and-test-selection)
and [native build routes](../../m3/docs/native-build.md#current-checkout-verification-routes).
One focused, **unexecuted** selection from the repository root is:

```sh
make CONF_NAME=CONFIG test TEST="test/jdk/jdk/internal/mindex/cb/com/m3/cb/CBProbe.java test/jdk/jdk/internal/mindex/descriptor/com/m3/descriptor/DescriptorTest.java"
```

Replace `CONFIG` with the actual matched-image configuration and ensure its jtreg setup
is available. This selection does not run the CB directory's native-null test or qualify
all String/JNI/GC behavior. External development probes also require an explicit internal
package export, as shown in CBTest; that does not make the package a supported public API.

The historical [run-image.sh](../../m3/tooling/cb/run-image.sh) needs `M3_JDK`,
`M3_BOOT_JDK`, `M3_JTREG`, a C compiler and Linux JNI headers/shared-library conventions.
At the reviewed pin it still names
`test/jdk/java/lang/String/MIndexMappedStringBackingTest.java`, whereas the current
mapped test is `M3MappedStringBackingTest.java`. It is not presented here as a verified
runnable whole-image recipe. Historical assertion counts and passes in the packet README
remain historical receipts, not exact-head results from this pass.

The reviewed `Class` paths still use their native/reflective implementations, including
native `isInstance`/`isAssignableFrom` and declared-reflection accessors. Text searches of
the fetched `Class.java` and `jdk.internal.misc.VM.java` found no M3 counterpart reference;
selected method reads do not establish a whole-HotSpot absence proof. The demonstrated
consumer is the explicit caller-owned bridge/probe. There is no basis here to claim
automatic Class attachment, Class-specific JNI routing or universal reflection replacement.
String JNI backing is a separate boundary documented by the
[JNI ingress/egress guide](../../m3/docs/mindex-jni-string-ingress.md).

Remaining gates include every ordinal/mask error, typed-wrapper identity checks,
record/nest ordering parity, unavailable-member conversions, changing access/redefinition
behavior, module/security/caller-sensitive behavior, exhaustive Class API coverage,
concurrent weak-boundary collection and the full runtime/GC/platform/performance matrix.
Nine complete source-body reviews do not close those gates or the wider package's
runtime qualification.
