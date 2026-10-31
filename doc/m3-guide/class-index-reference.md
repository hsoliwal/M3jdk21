<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Class-index snapshot contracts

[Guide](README.md) · [Runtime boundaries](runtime-owners.md) · [Class/member views](class-member-view-reference.md) · [Internal lanes and bridges](internal-metadata-lanes-reference.md)

This reference covers the complete [M3CI.java](../../src/java.base/share/classes/jdk/internal/mindex/M3CI.java) source body, including `Options`, `Symbols`, `Discovery`, `MemberDraft`, `NameIndex` and `Frozen`. The reviewed public source is `e7d21edf0f998c3c53ab0d0909972a4a97dfc471`, with blob `2acac4ad654a8af823fa46b483a42dd2f9c894c4`, unchanged from the P03 inventory at `278b221e8365ebeb6095a3a5643b38a0564258b5`.

`CI` means Class Index. It is an explicitly prepared, immutable, in-memory projection of selected JVM classes and their declared members. It is not a class-file loader, persisted image codec, global class registry, new Java SE API, or replacement for all `java.lang.Class`/reflection behavior. The `jdk.internal.mindex` package is in `java.base` and is not exported by its [module descriptor](../../src/java.base/share/classes/module-info.java). Public visibility on these internal classes does not establish supported application API status.

The test bodies and selected Class/VM/bridge boundaries described below were read; no compiler, build, policy verifier, recipe, jtreg, native oracle, benchmark or runtime was executed for this documentation pass. Source-contract coverage and runtime qualification remain separate.

## Snapshot admission and closure

`Options(int maxClasses, boolean retainWeakJvmBoundary)` rejects `maxClasses < 1`. `standard()` uses 16,384 classes and a weak JVM boundary; `compact()` uses the same maximum without weak-reference objects. The record's two accessors report those values; its generated record equality is configuration equality, not snapshot identity.

`compile(Class<?>... roots)` requires the array, converts it with `List.of`, and uses standard options and the no-op progress monitor. Null elements are consequently rejected. `compile(Collection<Class<?>>, Options, M3VI.Progress)` requires roots/options and a nonempty collection; null progress selects `Progress.none()`. It then calls `begin("M3CI.compile", -1)`, discovers the closure, freezes it, performs a final cancellation check and publishes the snapshot. `done()` is in a `finally` around discovery/freeze, but `begin` is before that `try`: a failing `begin` does not receive `done`. Other monitor exceptions propagate; a failing `done` can replace a pending result/error.

Discovery uses class identity, not name equality. A breadth-first queue follows these actual reflection edges:

- superclass and direct interfaces
- array component type
- nest host when different from the class itself
- declaring and enclosing class
- declared nested classes and all returned nest members
- returned permitted subclasses
- `Object.class` for each interface

The closure is **not** all classes visible to a loader. Field types, method parameters/returns/exceptions and annotation types are read for metadata but are not independently enqueued by those member relationships. A root may therefore pull in a large enclosing/nest/hierarchy closure; a count of roots is not the admitted class count. Null optional edges and already-seen identities are skipped. `enqueue` marks a new identity, rejects a closure larger than `maxClasses` with `IllegalArgumentException`, and only then queues it. No partially constructed index is returned on failure.

Loader identity is assigned in discovery order, with bootstrap loading (`null`) always space 0. Other defining loaders get successive local IDs. Classes are finally sorted by loader-space ID and then binary name. Reordering roots can change discovery-assigned loader IDs; same-named definitions in different loaders remain different rows. No ordering guarantee distinguishes two identities that compare equal on both sort keys.

During freeze, an identity map connects every admitted `Class` to its row. The builder reads names, module name, modifiers, flags, hierarchy, declared annotations, permitted classes and declared members. It retains existing immutable JDK `String` references as text owners, not a second encoded text payload. A temporary content-keyed map assigns text IDs by first insertion; a separate primitive `textOrder` gives lexical UTF-16 lookup. Reflection objects, temporary maps/lists, boxed rows, queues and intermediate row arrays are construction state and are not retained as structural entries by the finished `M3CI`.

Preparation is reflection work. It may cause metadata resolution and observes JVM/security failures; it is not an allocation-free scan or a guarantee of no class loading. The fixture test checks that preparing its explicitly defined class does not run that fixture's static initializer. That finite observation is not a universal no-effects promise for all reflection and annotation metadata.

Cancellation is polled once per class in discovery, once per class during initial freezing, and after freeze. Each of the first two loops reports one worked unit per class. Member enumeration, closure sorting, CSR construction, transitive traversal and fingerprinting have no per-element cancellation checkpoints. `maxClasses` does not bound total members, text lengths, graph edges, construction heap or elapsed time. Callers must supply a stable roots collection and coordinate lifecycle changes themselves.

## Stored lanes and observed order

All retained authority is private/final. The private `M3CI(Frozen)` constructor adopts builder-owned arrays; there is no external array-taking constructor.

| Lane family | Exact retained fields and meaning |
| --- | --- |
| Boundary and text | `boundary`, `texts`, `textOrder`: weak/live access separated from immutable names; text IDs are snapshot-local |
| Class identity and presentation | `loaderSpaces`, `binaryNameIds`, `simpleNameIds`, `canonicalNameIds`, `packageNameIds`, `moduleNameIds`, `typeNameIds` |
| Class facts | `modifiers`, `flags`, `superclassRows`, `componentTypeRows`, `nestHostRows`, `declaringClassRows`, `enclosingClassRows`, `structuralHashes` |
| Relationships | `interfaceOffsets/interfaceRows`, `directSubtypeOffsets/directSubtypeRows`, `supertypeOffsets/supertypeRows` are compressed sparse rows (CSR) |
| Class annotations/permits | `classAnnotationOffsets/classAnnotationNameIds`, `permittedOffsets/permittedNameIds` retain type names, not annotation values or class bytes |
| Binary-name reverse index | `nameKeys/nameOffsets/nameRows`: sorted numeric text IDs and each name's class rows |
| Declared members | `memberOffsets`, `memberDeclaringRows`, `memberKinds`, `memberNameIds`, `memberDescriptorIds`, `memberModifiers`, `memberFlags`, `memberStructuralHashes` |
| Member annotations/exceptions | `memberAnnotationOffsets/memberAnnotationNameIds`, `memberExceptionOffsets/memberExceptionDescriptorIds` |
| Digest | `fingerprintSha256` is derived snapshot metadata, not an admission certificate |

Class rows use the discovery/sort order above. Interfaces preserve `Class.getInterfaces()` declaration order. Permitted-subclass names preserve the order returned by `Class.getPermittedSubclasses()`. Annotation type-name IDs are sorted and deduplicated **numerically**, not alphabetically. Exception descriptors preserve reflection's returned order.

`admitMembers` visits declared fields, methods, constructors, then record components. `M3Descriptor` supplies VM descriptor spellings; constructors retain `Constructor.getName()`, not a fabricated `<init>` lookup name. Record components have modifier lane 0. A member's annotations are its declared annotation type names; fields/components have no exception-descriptor entries. No annotation values, generic type graph, parameter names, executable body or reflective access override is frozen.

Final member order is owner class row, insertion-assigned name ID, kind code, then insertion-assigned descriptor ID. It supports binary search on names inside a class's contiguous member range. It is not source declaration order, alphabetical name order, reflection-array order or a globally canonical overload ordering. See [typed member views](class-member-view-reference.md#declared-members-and-order) for the wrapper-specific operations.

`reverseHierarchy` counts superclass/interface edges and returns each parent's direct subtype rows sorted numerically. It does not synthesize array-covariance subtype edges or the interface-to-Object edge. `transitiveSupertypes` uses a fresh `BitSet` and stack per class, includes the class itself, follows superclass/interfaces and explicitly includes Object for interfaces. Each resulting set is emitted in ascending row order. Direct-subtype and transitive-supertype APIs therefore describe different relations.

## Identity and live boundary

`boundary()` returns the separate `M3Boundary`. Standard snapshots hold weak mirrors/loaders; compact snapshots keep counts only. See [the full weak-boundary contract](mapped-string-storage-reference.md#m3boundary-optional-weak-jvm-metadata). A retained snapshot need not keep the originating class loader alive. Immutable names and lanes remain readable after a weak referent disappears, but round trips can become unavailable.

`fromClass(type)` requires a non-null `Class`, obtains its actual defining loader's local space, looks up that space plus binary name, and finally requires the weak mirror to be the **same `Class` object**. An unknown loader, missing name, collected mirror or compact boundary yields `Optional.empty()`. Even bootstrap classes cannot round-trip from a compact snapshot: loader space 0 alone is insufficient without a live mirror. No fallback `Class.forName`, loader lookup or name-based substitution occurs in this method.

Rows, symbol IDs, loader spaces and the structural hashes that include those integers are owner-local. Pair a row with its `M3CI` owner. Two indexes containing equal names or equal numeric rows do not thereby share runtime type identity. Hidden-class descriptor/name spellings do not make those classes portable or resolvable in another VM session.

`resolveReflectiveElement(memberRow)` obtains the declaring class through the weak boundary, then scans the corresponding declared reflection array. Field, method and record-component matches require exact name and descriptor; constructors require exact constructor name and descriptor. No live owner or no exact member gives an empty optional. `LinkageError` and `SecurityException` from the resolution scan are converted to empty; unrelated failures propagate. Returned reflection objects are newly obtained boundary results, not a retained member-object cache, and this method does not call `setAccessible` or invoke a member.

`M3CI` has no invalidation, update, close or synchronization method. Immutable metadata is stable after construction, but this does not freeze live VM definitions or weak-reference availability. The caller-owned [M3CB bridge](internal-metadata-lanes-reference.md#class-bridge-m3cb) is where synchronized modes, declarations, epochs, explicit retirement and authoritative name/assignability fallback live. Its owner must invalidate before relevant redefinition/retransformation/access changes. Neither owner installs automatic JVMTI/ClassLoader hooks.

## Lookup and query surface

Unless the early-return cases below apply, class/member row access uses `Objects.checkIndex`, and ordinal access checks the size of the selected CSR/range. Invalid coordinates throw `IndexOutOfBoundsException`. Relationship row `-1` denotes absent/unindexed data; it is not a usable row.

| Declared entry points | Contract |
| --- | --- |
| `size()`, `memberSize()` | Class and total declared-member counts |
| `classAt(row)`, `memberAt(memberRow)` | Bounds-checked, newly allocated owner/row view; no wrapper cache |
| `loaderSpaceId`, `binaryName`, `simpleName`, `canonicalName`, `packageName`, `moduleName`, `typeName` | Class identity/presentation from frozen lanes; only canonical and module names use `Optional`, empty when their stored ID is negative |
| `modifiers`, `flags`, `structuralHash64` | Raw frozen facts, with the separate flags vocabulary rather than a replacement for Java modifier bits |
| `superclassRow`, `componentTypeRow`, `nestHostRow`, `declaringClassRow`, `enclosingClassRow` | Stored relation row or `-1` |
| `interfaceCount/interfaceRow`, `directSubtypeCount/directSubtypeRow`, `supertypeCount/supertypeRow` | Checked CSR count/ordinal access in the orders described above |
| `classAnnotationCount/classAnnotationTypeName`, `permittedSubclassCount/permittedSubclassName` | Checked type-name metadata; a name does not promise a live Class mirror |
| `classRows(binaryName)` | Requires a non-null `CharSequence`, compares exact UTF-16 content, and returns a fresh array of all same-name rows across loader spaces; a miss returns a new empty array |
| `classRow(loaderSpace,binaryName)` | First returns `-1` for an out-of-range loader space; otherwise scans `classRows` for that space. Because of that ordering, an invalid loader space can return `-1` without examining a null name |
| `declaredClassRows(classRow)` | Two whole-index scans; returns discovered rows whose declaring-class row equals the requested row, in ascending row order |
| `nestMemberRows(classRow)` | Two whole-index scans for matching nest-host row, including the host when present; empty for a negative host |
| `permittedSubclassRows(classRow)` | Resolves each recorded permitted name in the queried class's loader space and omits unresolved names; preserves the surviving name order |
| `memberCount(classRow)`, `memberAt(classRow,ordinal)` | Count and checked view of a class's contiguous declared-member interval |
| `memberRows(classRow,name)` | Validates class then name; UTF-16 text lookup followed by numeric lower/upper bounds; fresh array of all matching global member rows |
| `memberRows(classRow,kind)` | Validates class then non-null kind; two linear scans of its member interval; fresh row array in stored order |
| `memberRow(classRow,kind,name,descriptor)` | Requires kind/descriptor; if descriptor text is absent, returns `-1` before validating class row/name. Otherwise finds the first exact kind/name/descriptor row or `-1` |
| `memberDeclaringClassRow`, `memberKind`, `memberName`, `memberDescriptor`, `memberModifiers`, `memberFlags`, `memberStructuralHash64` | Checked reads of member lanes; kind decoding uses `M3MemberKind.fromCode` |
| `memberAnnotationCount/memberAnnotationTypeName`, `memberExceptionCount/memberExceptionTypeDescriptor` | Checked member CSR count/ordinal access |
| `fromClass`, `resolveReflectiveElement`, `boundary` | Live boundary operations described above |
| `fingerprintSha256`, `fingerprintSha256Hex`, `primitivePayloadBytes` | Digest exports/accounting described below |

`isAssignableFrom(supertypeRow,candidateRow)` requires both rows from this owner. Equal rows return true. If both are arrays, it recursively compares indexed component rows: if either component is primitive, both components must be the same row; otherwise it recurses. For other cases it binary-searches the candidate's transitive-supertype CSR. This covers array covariance without pretending Object[] is String[]'s direct superclass. It is a relation on the prepared closure, not permission to bypass live access, linking, casting or initialization rules.

Package-private `binaryNameKeyCount`, `binaryNameKey` and `rowsForBinaryNameKey` expose the reverse index to trusted sibling code; key order is numeric symbol-ID order, and row exports are copies. `uniqueMemberNameIds` clones, sorts and deduplicates all member name IDs. These are not a separate search engine. M3CI declares no regex/fuzzy class search, member-value query, ranking or query-result cache.

## Hashes, allocation and format boundaries

`structuralHash64` and `memberStructuralHash64` are FNV-style 64-bit candidate hashes using `HASH_SEED = 0xcbf29ce484222325` and `HASH_PRIME = 0x100000001b3`. `hash(long,int)` mixes four bytes least-significant first. Class hashes include its local identity/name/relationship lanes, interface IDs, annotation IDs, permitted IDs and each declared member's kind/name/descriptor/modifiers/flags. They do not independently include each member's annotation/exception lanes. Member hashes include owner row, kind/name/descriptor/modifiers/flags and annotation/exception IDs. Neither is collision-free or a digest of authenticated class bytes.

`fingerprint(Frozen)` computes SHA-256 over the text count, length-prefixed UTF-16 text spellings, and the class/member/hierarchy/reverse-index/annotation/exception lanes, including structural-hash lanes. Integers/longs use big-endian digest byte order; each UTF-16 unit contributes high then low byte, and member-kind bytes are appended directly. The derived lexical `textOrder` and live weak boundary are excluded. The input stream is an implementation-defined digest construction, **not** an externally versioned file format. It does not include method bytecode, annotation values or the entire loaded definition. `NoSuchAlgorithmException` becomes `IllegalStateException("SHA-256 unavailable")`.

`Frozen.withFingerprint` clones the digest while sharing the already constructed lanes. `fingerprintSha256()` returns another clone; `fingerprintSha256Hex()` renders it. The repeated-compilation test uses the same root list. It does not establish a stable cross-build/class-loader/cross-image canonical digest or a content-addressed resolver. A fingerprint is neither class authentication nor authorization to reuse compiled code.

`primitivePayloadBytes()` is the source-defined subtotal of 4-byte entries in its enumerated integer lanes, 8-byte entries in both structural-hash lanes, and member-kind bytes. In particular it omits `textOrder` and fingerprint bytes as well as reference arrays, text payload, array/object headers, temporary construction state and weak-boundary objects. Do not present it as measured retained heap, total primitive storage, native memory or a maximum allocation budget. The helper uses checked total-length/addition operations; it does not measure a runtime heap.

There is no `read`, `write`, `open`, magic number, schema-version comparison, checksum admission, endian-negotiation or malformed-image parser in M3CI. Its admission boundary is live `Class` roots plus bounded reflective closure. Actual mapped-image/header validation belongs to [mapped String storage](mapped-string-storage-reference.md#opening-a-mapped-textbyte-snapshot); version ordering belongs to [M3VI](internal-metadata-lanes-reference.md#version-index-m3vi). These mechanisms must not be conflated merely because each uses metadata or integers.

## Complete helper and failure map

The public surface above plus the following map accounts for the file's implementation helpers and nested storage types; compiler-generated record/Object members are not separate hand-written kernels.

| Helper/declaration | Responsibility and important limit |
| --- | --- |
| `NONE`, `HASH_SEED`, `HASH_PRIME`; private constructor | Absent-row sentinel, hash constants and adoption of a completed `Frozen` bundle |
| `checkClassRow`, `checkMemberRow`, `text`, `optionalText` | Bounds checks and negative-ID optional handling |
| `discover`, `enqueue`; `Discovery(classes,loaders,loaderIds)` | Identity closure and temporary strong reflection/loader state, discarded after freeze |
| `freeze`; `MemberDraft(owner,kind,nameId,descriptorId,modifiers,flags,annotationNameIds,exceptionDescriptorIds)` | Populate class lanes, admit/sort members, build CSR/name indexes/hashes and weak boundary; drafts are construction-only |
| `admitMembers` | Wraps `LinkageError`/`SecurityException` inside member enumeration as `IllegalArgumentException("cannot freeze declared members of …", cause)`; does not publish partial member rows |
| `annotationNameIds`, `permittedNameIds`, `exceptionDescriptorIds` | Declared annotation names sorted/unique by ID; permits and exception descriptors preserve observed order |
| `reverseHierarchy`, `transitiveSupertypes` | Direct and transitive graph derivation described above |
| `buildNameIndex`; `NameIndex(keys,offsets,rows)` | Temporary TreeMap groups numeric name IDs; emits primitive reverse-index lanes |
| `classHash`, `memberHash`, `hash`, `fingerprint`, both `update` overloads | Candidate hash/digest computation, with differing byte orders specified above |
| `offsets`, `flatten`, `prefixSum` | CSR prefix sums and copies. `offsets` uses `Math.addExact`; `prefixSum` uses ordinary integer addition |
| `sortedUnique`, `filled`, `totalLength` | In-place numeric sort/dedup of builder arrays, sentinel array creation and checked integer-lane length total |
| `rowOf`, `requiredRow` | Null/missing class becomes `-1`; required direct-interface closure missing throws `IllegalStateException` |
| `findText`, `compareText` | Binary search through `textOrder` using exact code-unit comparison and length; no input `toString` materialization or global interning |
| `lowerBound`, `upperBound` | Null/range-checked half-open integer binary searches over already sorted lanes |
| `findField`, `findMethod`, `findConstructor`, `findRecordComponent` | Fresh live reflection scans for exact name/descriptor identity |
| `Symbols.ids`, `Symbols.values`, `add`, `order` | Builder-only HashMap and ArrayList; first insertion owns ID, equality reuses it; `order` sorts boxed indices then emits an int array |
| `Frozen` and `withFingerprint` | Internal record of every retained lane, boundary and digest; not a serializable image declaration or public mutation surface |

Only the described member-admission errors are wrapped. Discovery/class-level reflection errors, other runtime exceptions and allocation failures can escape; the source does not silently skip inaccessible classes or fall back to a smaller snapshot. `resolveReflectiveElement` has a deliberately different empty-on-unavailable policy. Integer-overflow checks in selected helpers do not make all cumulative size arithmetic checked. The index itself installs no global cache, background worker, synchronization, native allocation or native cleanup obligation.

## Actual tests and runtime prerequisites

[CBProbe](../../test/jdk/jdk/internal/mindex/cb/com/m3/cb/CBProbe.java) and [VmFixture](../../test/jdk/jdk/internal/mindex/cb/com/m3/cb/VmFixture.java) are the direct source evidence read for this owner:

- `bridge` defines same-named classes in distinct loaders, checks delegation, hidden/bootstrap/primitive/array roots, weak round trips, bridge modes and absence of fixture initialization; compact snapshots exercise authoritative bridge fallback
- `core` compares class names/flags/interfaces, complete declared-method sets and field/method round trips with reflection; checks records, sealed permits, hierarchy including arrays, owner mismatch refusal, 1,000 negative name lookups, repeated-root fingerprint stability, different-closure digests and the class-count budget
- `bridge` also supplies a cancellation monitor and checks exactly one `done` call after cancellation in preparation
- `unload` retains a bridge but drops fixture loader/class references, then uses bounded GC polling to assert both weak referents clear; this is a test assertion, not deterministic GC progress guaranteed by the API

The test header requests `java.base/jdk.internal.mindex`, class unloading and separate interpreter/mixed VMs. It is not an exhaustive test of every scalar accessor, malformed annotation, counter overflow, every primitive/array geometry, definition race, all loader implementations or all JVM platforms. Its `ClassFormatError` check invokes `ClassLoader.defineClass` on invalid bytes; it does not test a nonexistent M3CI image reader.

The [CB tooling README](../../m3/tooling/cb/README.md) documents recipe/strict-compile/image routes and historical assertion counts. [run-image.sh](../../m3/tooling/cb/run-image.sh) requires a matched `M3_JDK`, boot JDK and jtreg JAR, compiles a Linux JNI fixture, runs CB/mapped tests and String JNI oracles, and separately requests default-mode C1/C2 probes. This authoring pass did not run those commands or verify the README's historical receipts as current exact-commit results. A stock-JDK patch-module test is not a complete matched-fork image proof.

The inspected [Class.java](../../src/java.base/share/classes/java/lang/Class.java) and [VM.java](../../src/java.base/share/classes/jdk/internal/misc/VM.java) do not install M3CI/M3CB dispatch or automatic counterpart attachment. The explicit bridge reads original Class operations where required. The full matched image, Java SE/reflective behavior, class unloading/redefinition, GC/CDS/JVMTI/JFR, native/HotSpot Class routing, platform matrices and CPU/retained-heap/native-memory evidence remain separate gates. String storage's experimental VM admission does not promote this metadata family.

The [canonical porting invariant](../../m3/docs/m3jdk21-porting-invariant.md), [name map](../../m3/docs/name-mapping.json), per-file licenses and [full-family receiver](../../m3/synexia-import/current-full-borrow/README.md) remain authoritative. Documentation does not advance the ordered String → arrays → collections → AST/compiler → remaining-families receiving phases or create a runtime dependency on the donor workspace.
