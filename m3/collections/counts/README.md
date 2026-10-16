# M3 counted collections and allocation-safe storage

This successor to the four-library interop slice adds a primitive long-count bag, a non-null Long Guava Multiset facade, explicit JDK/Guava/Eclipse imports, a parallel collector, and paired Java/JNI checked count-lane algebra. It also repairs partial rehash publication in the existing M3LongLongHashMap, M3LongHashSet and M3LongLinkedHashSet. Existing public declarations are unchanged. Allocation failures cannot publish half-replaced lane sets; optional post-removal compaction failure no longer splits a completed removal from its containing bag's aggregate update.

## Recipe custody

All 16 delivered Java/native/catalogue/provenance/policy files are exact results from the existing OpenRewrite Java and text snapshot engines. The recipe uses their original Git blob identities, not a second transformation implementation. The standalone Maven crate is `m3/collections/counts/recipe`; Synexia registers this receiver specialization under its existing `m3-four-library-bags` donor crate. The donor is Synexia commit `6c33a5772a12ca99c8bff6a02403b28be4202bb3`. Public M3 names remain receiver-owned.

The text engine runs against module-relative source paths; Java runs against repository-relative paths. The tested materializer validates the entire closure first, refuses symlinks and non-preimage occupants, and writes only actual recipe results. Use an exclusively owned checkout. Individual replacement uses an atomic move where supported; the complete set is not a filesystem transaction. Fix templates and hashes, rerun the actual recipe, and verify output. Never patch generated output manually.

```sh
mvn -B -ntp -f m3/collections/counts/recipe/pom.xml test
mvn -B -ntp -f m3/collections/counts/recipe/pom.xml -Dm3.counted.target="$PWD" test
```

`verify.py` is a read-only verifier, not a source transformation framework. The workflow obtains the real pinned Guava/Eclipse/Spring dependency closure and JUnit console, executes the Maven recipe, checks replay against committed bytes, builds the module and optional adapters, compiles JNI with UBSan, runs differential tests and original/candidate heap-pressure controls, checks public signatures, and verifies java.base-only core dependencies. Missing native proof is an error, not a skip. The recipe tests also reject uncatalogued targets.

## Contracts and memory

M3LongBag has checked long counts and aggregate size. Add/remove require positive amounts and return NEW counts; setCount returns OLD. Its algebra returns independent snapshots, never expanded occurrences. Intersection scans the smaller backing capacity; trimming builds a replacement before publication but retains the map's minimum eight slots.

M3GuavaLongMultiset implements Guava's int-count interface for non-null Long keys. Add/remove return OLD counts, each count is bounded by Integer.MAX_VALUE, and size saturates. Element and entry sets are backed; entries themselves are immutable snapshots. Removable iterators keep primitive distinct-key snapshots, not occurrence arrays. Public projections can box and allocate; retained storage does not keep a boxed count/key/entry per distinct value. This is not a HashMultiset concrete-class, null-key, serialization or concurrency substitute. Bulk operations are not transactional on arbitrary callback/overflow failures.

M3LibraryBags imports JDK occurrences, Guava counted entries and real Eclipse primitive occurrence callbacks. Eclipse import is a copy, not an implementation of the entire Eclipse LongBag interface. The parallel collector has separately owned partials and does not declare CONCURRENT.

M3CountLanes combines already aligned, stable nonnegative count slices. Java and JNI reject individual and aggregate long overflow and do not modify inputs. Native invocation is explicit after loading a trusted compiled library using System.load; unavailable native code throws instead of silently falling back. Native scratch is three 1024-jlong arrays (24 KiB), plus one Java result array. Alignment and boundary overhead are not hidden in a performance claim.

## Evidence and limits

Actual local SDK execution passed eight recipe tests, 16 new Java/JNI tests, the 42 previous interop tests and 13 existing long-core tests. All 80 core and five optional interop main units compiled with Java 21 warnings as errors. Four deliberately heap-limited original implementations fail the same probes that the candidates pass. Public javap declarations of all three repaired owners match. Local Maven was unavailable; hosted Maven status must be established from the exact-head workflow, not inferred from local SDK proof.

The complete core test sources compile, but only the long-core runtime slice is claimed; a broad runtime attempt did not complete locally. Unrelated int-lane owners, object bags, bimaps, ranges, complete Eclipse facades, concurrency/reference/serialization, dependency-security review, end-to-end benchmarks and installed-M3JDK image delivery remain unqualified. The legacy manifest and source-qualified SDK/release gates are not modified or bypassed. No STRING-phase advance, full-library completion, automatic merge or speedup is claimed.

The searchable catalogue is a receiver sub-ledger of the existing Synexia bag catalogue. Its LeetCode350, HackerRank Frequency Queries and GeeksforGeeks sorted-union entries are separate category evidence, not copied solution code. Run the generated M3CountedCatalogue with the TSV path and an optional literal query. Exact donor mapping and licenses remain in provenance.json and packaged metadata.
