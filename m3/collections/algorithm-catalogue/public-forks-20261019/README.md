# Public collection forks in hsoliwal

Observed on 19 October 2026. The public-fork search returned **40 repositories** on page 1 and none on page 2, with 100 requested per page. Each result was individually checked for public visibility and fork status. [PUBLIC_FORKS.json](PUBLIC_FORKS.json) records all 40, their upstreams, default branches, detected repository licenses and relevance categories. Private repositories are excluded.

The source audit is narrower: **21 selected implementation files across eight forks**, plus selected licenses, branch listings and existing packet documentation. Metadata classification of the other forks is not a claim that every source file was reviewed. No code was imported, no repositories were cloned, and no builds, tests or benchmarks were run for this inventory.

| Fork | Useful existing work | Remaining limit |
| --- | --- | --- |
| [Guava](https://github.com/hsoliwal/guava/tree/3aea962167e102158aaf6ef39ce3c2c17e8a9f15) | Array-backed HashBiMap; packed key/count/hash/link storage | ObjectCountHashMap collision index uses HashMap; CompactHashMap flooding fallback uses LinkedHashMap |
| [Commons Collections](https://github.com/hsoliwal/commons-collections/tree/03f5ae07596f2fd648419cd69e7c4f3dc01d7420) | TreeList payload, link, rank, height and state arrays; free-slot reuse | AbstractHashedMap still stores chained HashEntry objects |
| [Eclipse Collections](https://github.com/hsoliwal/eclipse-collections/tree/489c8e63b973aaf52eb87f08f5908a152a4b8f22) | UnifiedMap interleaved slots and array collision buckets; FastList dynamic array | Relocated TreeSortedMap still delegates storage to java.util.TreeMap |
| [Spring](https://github.com/hsoliwal/spring-framework/tree/d8e77dbd0160231dd231496a49a5d53752187013) | Case-insensitive ordering, multivalue aliasing and reference-lifecycle contracts | Inspected maps retain JDK map nodes or explicit reference/entry chains |
| [Swift Collections](https://github.com/hsoliwal/swift-collections/tree/b905c889ff75059b8ef0bde480fe55948147b1e6) | Bitmap rank/select, deque offsets and segmented access | Requires a Java/JNI port; hash-tree storage still has object nodes |
| [Lucene](https://github.com/hsoliwal/lucene/tree/ec6ecef838e3c1f93ce19d1c353f1e942efab7b4) | CharArrayMap parallel arrays and character-slice lookup; FixedBitSet word masks | Specialized mutation, ownership, range and dependency contracts require adaptation |
| [Jackson collection datatypes](https://github.com/hsoliwal/jackson-datatypes-collections/tree/5631f637e626c033319a1e5ac11ecde8db5d319e) | Collection serializers, deserializers and tuple registration | Integration code delegates to actual collection owners; Jackson 3 namespace |
| [M3jdk21 / OpenJDK](https://github.com/hsoliwal/M3jdk21/tree/a3e88976ee8664d1ef5d0cade96aec1206b2133d) | ArrayList dynamic storage and bulk copies; ArrayDeque ring offsets | Inspected TreeMap retains Entry nodes; source files retain GPL-2.0 with Classpath exception |

Guava and Commons Collections already have `m3/array-storage-20261019` branches. Their pinned heads above are distinct from their default branches. Existing packet descriptions identify additional work, but their reported validation was not independently rerun here. Eclipse's inspected default already uses relocated packages; relocation itself does not establish M3 array ownership.

The JSON audit files and candidate reuse ledgers record immutable commits, exact source paths and Git blobs. These are **candidate classifications**, not authorization to bypass contract, license or execution gates. Preserve applicable per-file notices: repository-level license detection is only an index. Eclipse's inspected files retain EPL-1.0/EDL-1.0 terms; Swift retains its Swift exception; OpenJDK source must not be relabeled as Apache-2.0.

The immediate reuse order is existing M3 fork implementations, Eclipse's array owners, Lucene's specialized array/bit atoms, then qualified Swift ports for uncovered mechanisms. Preserve public interfaces, callback and exception behavior, iteration, aliasing, serialization, extension hooks, GC and concurrency contracts when replacing storage. Stored entry nodes hidden in collision fallbacks remain part of the storage audit. API-required temporary entries remain permitted.

The LeetCode/HackerRank [challenge catalogue](https://github.com/hsoliwal/M3jdk21/blob/48b31d40d4e38800c4de6a40a567af7e1f97d089/m3/collections/algorithm-catalogue/challenge-patterns-20261018/README.md) supplies algorithm categories for comparison. Neither that research nor this inventory establishes peak performance. Selection still requires applicable donor licensing, executed contract proof and comparable measurements.

Exact-name public-fork searches for fastutil, PCollections, HPPC and Agrona returned no results in this observation. This does not say those projects or private/non-fork copies are absent elsewhere.
