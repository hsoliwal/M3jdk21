<!-- SPDX-License-Identifier: Apache-2.0 -->
# DOOP API-entry facade cache (2026-10-18)

This receiver is stacked on [M3JDK21 #1330](https://github.com/hsoliwal/M3jdk21/pull/1330), itself stacked on [#1325](https://github.com/hsoliwal/M3jdk21/pull/1325). It does **not** promote the 59 package-renamed JDK source files from #1325 into the runtime. #1325 is a source-custody pass; the storage conversion and compatibility work are separate.

## Non-negotiable storage invariant

Canonical collection state is payload lanes plus primitive slot/index/topology/state lanes. There is no per-mapping `Node`, `Entry`, `Bucket`, or `Cell` object.

- Generic sequence payloads occupy dynamic `Object[]` lanes.
- Hash-index buckets store integer `physical slot + 1`; zero and negative values represent empty/tombstone index states.
- Parallel `int[]` and `byte[]` lanes manage hashes, positions, free-slot links, occupancy, owner flags and masks.
- Reuse of a freed physical slot is an index/lifecycle operation, not recycling a Java entry object.

The LFU cache is an API projection cache, not a storage engine. It is allocated lazily on the first API request that materializes a map entry. Both lookup structure and policy metadata are stored in primitive lanes; its `Object[]` value lane holds only those API objects that were explicitly requested. The cache is bounded (128 live facades and 128 immutable snapshots per map view).

## Facade identity and contracts

- The cache key is a primitive pair: `(physical slot, owner revision)`. It does not call client key `hashCode()` or `equals()`, allocate key tuples, or change callback timing merely to return an entry.
- Repeated requests at an unchanged slot/revision can reuse a live facade. That facade resolves the current mapped value through its owner; it is never retargeted to a different key.
- Detached navigable-map snapshots are cached only while slot, revision, key identity and value identity still match. A new mapping value gets a new snapshot; an older returned immutable snapshot stays detached and unchanged.
- Erase/poll paths evict cached references for the affected physical slot; clear drops all cache arrays/references. Owner revisions also prevent stale coordinates from being reused after structural changes.
- Weak-key maps opt out of strong facade caching. Retaining a live facade strongly retains its key and would violate weak-key reclamation expectations.
- The LFU policy uses a least-recently-touched tie break. Evicted API objects that callers still hold are not repurposed for another slot.

## Algorithm / donor decision

The existing M3 slot/index owners are the primary donor because they already provide the intended packed layout and view semantics. This change applies the conventional open-addressed index and LFU/LRU eviction pattern to API materialization only; it does not copy JDK `HashMap.Node` storage or port it into a second collection engine. OpenJDK source custody and attribution remain recorded in #1325. No third-party challenge solution is copied.

See `DONOR_EVALUATION.tsv` in this directory for category-level comparison and retained reasoning.

## Verification state

Added/updated tests cover LFU selection, slot eviction, revision isolation, facade reuse, detached snapshot stability, different-key non-aliasing and entryless storage-lane inspection. The current execution environment does not have Maven available, so **this PR does not claim a local Maven/JUnit pass**. GitHub checks and the strict module receiver must pass before promotion.

No JNI entry point is added or changed in this receiver. The Java facade cache contains object references and must not be moved to native code; Java-visible key/value semantics stay in the Java owner. Native array/index parity and full JDK/JCK/serialization/concurrency gates remain independent.
