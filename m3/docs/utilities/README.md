<!--
Copyright 2026 Hitesh Soliwal and contributors
SPDX-License-Identifier: Apache-2.0
-->
# M3JDK21 utility families

M3JDK21 is one runtime. This catalogue makes its existing entry points easier to find and records three coherent additions: `java.util.Streams`, `java.io.IOUtils` and `java.lang.reflect.Reflections`. The separate sorting packet contributes `java.util.Sorts`. Established owners such as `Objects`, `Strings`, `Arrays`, `Collections`, `Files` and `Paths` retain their responsibilities.

[FAMILIES.tsv](FAMILIES.tsv) contains **103 registered families** across **14 JDK modules and the existing `m3/collections` component**. It includes static helpers, factories and types that already own an operation. It is a finite discovery and source-ownership catalogue, not a declaration that every conceivable utility or future API has been implemented. It does not enumerate every overload or replace the individual owners' documentation.

## One owner for each recorded problem

A stable category ID identifies each recorded family. Each row names one owner, its component and its actual source path. Category IDs describe responsibilities, such as `filesystem.operations`, `filesystem.path_factories` and `filesystem.path_values`; they do not force unrelated classes into a single facade.

The registry distinguishes three roles:

| Role | Meaning | Examples |
| --- | --- | --- |
| `utility` | Existing static helpers or constants for a coherent operation family | `Objects`, `Arrays`, `Files`, `StandardCharsets` |
| `factory` | An entry point primarily used to construct or discover another service or value | `Paths`, `Executors`, `DocumentBuilderFactory` |
| `type-owner` | The established value, container or service type owns its operations and any factory methods | `BigDecimal`, `ByteBuffer`, `HttpClient`, `Optional` |

For example, `Paths.get` remains a path factory, `Path` owns path values and composition, and `Files` owns filesystem operations. A new empty `FileUtils` facade would add no independently justified responsibility. Likewise, `Pattern` owns regex compilation, `DateTimeFormatter` owns date/time formatting, and `MessageDigest` owns digest state and provider selection.

Names follow their established packages. Stream entry-point adaptation belongs in `java.util.Streams`; byte/character transfer belongs in `java.io.IOUtils`; reflected-member selection and explicit handle adaptation belong in `java.lang.reflect.Reflections`. The existing internal `java.util.stream.Streams` is a separate implementation class and is not promoted or replaced by the new public `java.util.Streams`.

Public JDK packages and module exports remain their existing owners. The M3 collection sources remain the existing component within M3JDK21. Their distinct Java class names and contracts do not constitute a second runtime or an integration overlay.

## Source and catalogue invariant

The baseline is [hsoliwal/M3jdk21 revision 2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49](https://github.com/hsoliwal/M3jdk21/tree/2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49). Registry construction checked the 99 existing owner inputs against exact Git blobs, the supplied inventory of 23,335 pinned `src/` paths and the pinned M3 collection tree. Four rows refer to the separate Sorts addition or this packet's three new utility sources.

The byte-buffer row records `src/java.base/share/classes/java/nio/X-Buffer.java.template`. The pinned JDK generates `ByteBuffer` from that source template; an invented checked-in `ByteBuffer.java` path would be incorrect. The schema marks this as `generated-template`. Other rows identify their actual Java source file.

Each row records:

| Column | Meaning |
| --- | --- |
| `category_id` | Stable lowercase responsibility ID |
| `owner` | Fully qualified Java owner |
| `owner_kind` | Utility, factory or type owner |
| `component` | Existing JDK module or `m3/collections` component |
| `source_path` | Normalized repository-relative source path |
| `source_kind` | Java source or the explicitly recognized generated-buffer template |
| `status` | Source lineage within this change sequence |
| `operations` | Searchable existing operation names, not an exhaustive API census |
| `contract_notes` | The distinction callers must retain |
| `baseline_git_blob` | Exact existing source blob, or `-` for an added source |

The statuses describe source lineage, not a test or release badge:

- `established`: the owner is present at the pinned baseline.
- `refined-collections`: an existing owner receives the separately qualified collection convergence or callback-state refinement.
- `added-sorts`: the new owner comes from the separate sorting packet.
- `added-utilities`: the owner is one of this packet's three utility additions.

The recipe invariant remains per file: identify the actual owner, preserve source rights, admit an exact existing preimage or an absent new path, qualify the behavioral change through the existing recipe, execute the relevant owner tests, and retain an actual receiving-tree receipt. Listing a class in this catalogue does not change it or count as a test of it.

The Python catalogue tool checks the schema, unique category/owner assignments, package-to-source mapping, existing blob pins for sparse fallback and the available source declarations. It rejects unknown statuses and kinds, malformed paths, conflicting ownership rows, missing added/refined files and baseline blob mismatches. Available source files also undergo operation-name presence checks. These are source/discovery checks; Java compilation and behavioral tests remain separate gates.

## Search and verify

The tool uses Python 3.10 or later and the standard library. Git is needed only when a sparse checkout lacks an unchanged registered source. A complete checkout is checked directly on disk.

From the repository root:

```sh
python3 m3/verification/utilities/catalogue.py search mask
python3 m3/verification/utilities/catalogue.py search filesystem
python3 m3/verification/utilities/catalogue.py search java.net.http --json
python3 m3/verification/utilities/catalogue.py verify --source-root .
```

`search` performs case-insensitive substring matching across all columns. An empty query lists all families. `--catalogue PATH` selects another schema-compatible catalogue; `--json` gives machine-readable results. Both commands return status 2 for an invalid catalogue.

`verify` reports separate counts for on-disk source checks and pinned Git-tree inventory proofs. In a sparse tree it invokes read-only `git ls-tree` at the catalogue's exact baseline revision for absent `established` sources, and checks the returned blobs against the row pins. It never treats that fallback as compilation, source hydration or runtime execution. Added utility/Sorts sources and refined collection sources must exist on disk.

The ordinary utility verification entry point is `m3/verification/utilities/verify.py`; its execution receipts record compilation and behavioral results separately from catalogue validation.

## New utility entry points

### Streams

`Streams.stream` provides sequential streams from a collection, iterable, iterator, enumeration or optional value. The `PrimitiveIterator.OfInt`, `OfLong` and `OfDouble` overloads preserve primitive traversal.

```java
import java.util.List;
import java.util.Streams;

List<String> selected = Streams.stream(List.of("bee", "ant", "bat").iterator())
        .filter(value -> value.startsWith("b"))
        .toList();
```

Source adapter acquisition occurs when the helper is called. Element traversal follows the returned pipeline and the source owner's binding/interference behavior. The helpers add no close handler. A resource behind an iterator or iterable remains the caller's responsibility. [STREAMS-IO.md](STREAMS-IO.md) records the API-reference review, resource contracts and executed Streams/IOUtils candidate qualification.

### IOUtils

Bounded copy requests no more input than its remaining limit:

```java
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOUtils;

var input = new ByteArrayInputStream(new byte[] {1, 2, 3});
var output = new ByteArrayOutputStream();
long copied = IOUtils.copy(input, output, 2); // 2
int next = input.read();                     // 3 remains
```

Whole-input copy delegates to the existing `transferTo` implementation. `readFully` requires the complete destination range and throws `EOFException` when input ends early, preserving any prefix already read. `contentEquals` compares the remaining sequences despite different bulk-read fragment sizes. The helpers borrow input/output objects and never close or flush them. They do not roll back transferred prefixes after an I/O failure.

Byte-to-text conversion requires a charset:

```java
import java.io.IOUtils;
import java.nio.charset.StandardCharsets;

// input is a caller-owned InputStream:
String text = IOUtils.toString(input, StandardCharsets.UTF_8);
```

The String decoding constructor's replacement behavior applies. Reader overloads already receive decoded UTF-16 character units.

### Reflections

Member selection preserves encounter order, duplicate member identities and the selected array range. Required modifier bits must all be present, forbidden bits must all be absent, and each visited member is observed once. Selection returns a detached unmodifiable list.

```java
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Reflections;
import java.util.List;

List<Method> publicInstanceMethods = Reflections.filterMembers(
        String.class.getDeclaredMethods(), Modifier.PUBLIC, Modifier.STATIC);
```

Handle factories borrow explicit caller authority:

```java
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Reflections;

// In a method that declares throws Throwable:
MethodHandle length = Reflections.unreflect(
        MethodHandles.publicLookup(), String.class.getMethod("length"));
int count = (int) length.invokeExact("m3"); // 2
```

The helper creates no privileged lookup and changes no accessibility flag. Existing caller-set overrides have exactly the effects of the corresponding `Lookup` operation. In particular, `unreflectVarHandle` ignores access override flags. Caller-sensitive lookup checks, final-field restrictions, receiver narrowing, variable arity and checked access failures remain with `Lookup`.

### Sorts and existing collection owners

The prior sorting packet supplies explicit algorithm selection, including TimSort, quicksort and the other registered finite in-memory families. The existing `Arrays` defaults remain available.

```java
import java.util.SortAlgorithm;
import java.util.Sorts;

int[] values = {3, 1, 2};
Sorts.sort(values, SortAlgorithm.TIM);
```

The `Sorts` and algorithm documentation define applicable domains, stability, auxiliary index state, numeric histogram limits and progress behavior. This utility catalogue does not widen those contracts or claim literally every possible sorting algorithm.

M3 collection views preserve the distinction between a live backed wrapper and a detached snapshot. `M3CollectionViews` owns live filtered reference views and explicit stable sorted snapshots; `M3FilteredViews` owns strict read-only live adapters; `M3MutableFilteredViews` owns mutation-capable navigable filtering. The existing compatibility facade `M3FilteredNavigableSet` continues to delegate to its canonical read-only owner; no duplicate implementation is created by this registry.

`M3LongIndexProjection` owns a detached primitive payload with index/offset views. `M3IndexMaskWindow`, `M3IndexMaskCompactor`, `M3IndexPathMasks` and `M3LongIndexPathMasks` own their existing aligned-state and path-selection contracts. Physical offsets, logical indices, signed strides and state masks remain distinct concepts. Arbitrary Java predicates and comparators stay in Java; optional JNI kernels receive the existing primitive lanes. Application state flags do not become object headers, garbage-collector roots or native object pointers.

## Rights and limits

This catalogue, guide and Python tool are original 2026 work by Hitesh Soliwal and contributors under Apache-2.0. They copy no third-party implementation. Existing runtime source headers remain authoritative: JDK-package utility additions retain GPL-2.0-only with the Classpath exception, while the existing M3 collection owners retain their own notices and Apache-2.0 provenance.

The catalogue preserves established source owners across text, numbers, arrays, collection views, ordering, streams, I/O, filesystems, time, encodings, networking, security, concurrency, reflection, management, logging, JDBC, XML, desktop, naming, preferences, RMI, scripting, compiler discovery and JFR. Installed service providers and optional capabilities remain discoverable at runtime; the presence of a factory does not imply that every provider or backend is installed. Platform, provider, native and runtime qualification claims belong to their actual execution receipts.
